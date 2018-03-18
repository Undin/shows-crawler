extern crate diesel;
extern crate itertools;
#[macro_use]
extern crate log;
extern crate log4rs;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_yaml;
#[macro_use]
extern crate server;
extern crate threadpool;

use diesel::*;
use diesel::pg::PgConnection;
use itertools::Itertools;
use server::commands::{Command, PermissionLevel};
use server::Components;
use server::models::Subscription;
use server::schema::shows::table as show_table;
use server::schema::subscriptions::table as sub_table;
use server::schema::users::table as user_table;
use server::schema::shows::dsl::{id as sid, shows, source_name, title};
use server::schema::users::dsl::{active, chat_id as cid, has_active_session, id as uid, superuser, users};
use server::schema::subscriptions::dsl::{show_id as sub_sid, subscriptions, user_id as sub_uid};
use server::telegram_api::{CallbackQuery, Chat, ChatType, Message, Update, User};
use std::cmp::min;
use std::convert::Into;
use std::fs::File;
use std::io::BufReader;
use threadpool::ThreadPool;

#[derive(Deserialize, Debug)]
struct Config {
    pub bot_token: String,
    pub database_url: String,
    #[serde(default = "default_threads")]
    pub threads: usize,
    #[serde(default = "default_update_timeout")]
    pub update_timeout: u32,
    #[serde(default = "default_update_batch_size")]
    pub update_batch_size: u32
}

const COMMANDS: &'static [Command] = &[
    command!("service_message", &["message"], "Send service message to all users. For example, `/service_message Hello everyone!`", PermissionLevel::Superuser),
    command!("sources", &[], "List supported sources", PermissionLevel::User),
    command!("shows", &["source"], "List TV shows for particular source. For example, `/shows lostfilm`", PermissionLevel::User),
    command!("statistics", &[], "Show bot statistics", PermissionLevel::Superuser),
    command!("subscribe", &["source", "show title"], "Create subscription to notifications about TV show. For example, `/subscribe lostfilm Daredevil`", PermissionLevel::User),
    command!("unsubscribe", &["source", "show title"], "Remove subscription. For example, `/unsubscribe lostfilm Daredevil`", PermissionLevel::User),
    command!("subscriptions", &[], "List your current subscriptions", PermissionLevel::User)
];

fn main() {
    log4rs::init_file("log4rs.yaml", Default::default()).unwrap();
    let config = read_config();

    let components = Components::new(config.bot_token, config.database_url);
    let thread_pool = ThreadPool::with_name("thread-pool".into(), config.threads);

    let mut update_id = 0i32;

    loop {
        match components.api.get_updates(config.update_timeout, config.update_batch_size, update_id) {
            Ok(update_response) => {
                let updates = update_response.result;
                for update in updates {
                    update_id = update.update_id + 1;
                    let components = components.clone();
                    thread_pool.execute(move || process_update(components, update));
                }
            }
            Err(error) => error!("{}", error),
        }
    }
}

fn read_config() -> Config {
    let config_file = File::open("notifier-server-config.yaml")
        .expect("can't find notifier-server-config.yaml file");
    serde_yaml::from_reader(BufReader::new(config_file)).unwrap()
}

fn default_threads() -> usize { 1 }
fn default_update_timeout() -> u32 { 20 }
fn default_update_batch_size() -> u32 { 100 }

fn process_update(components: Components, update: Update) {
    match update {
        Update {
            message: Some(
                Message {
                    chat: Chat { id: chat_id, chat_type: ChatType::Private },
                    from: Some(user),
                    text: Some(text),
                    ..
                }),
            ..
        } => process_user_command(&components, chat_id, &user, &text),
        Update {
            message: None,
            callback_query: Some(
                CallbackQuery {
                    from: user,
                    data: Some(data),
                    ..
                }),
            ..
        } => process_button_click(&components, &user, &data),
        _ => debug!("skip message")
    }
}

fn process_user_command(components: &Components, chat_id: i64, user: &User, user_text: &str) {
    info!("user {}: {}", user.id, user_text);

    let ref connection = *components.get_connection();
    set_has_active_session(connection, user.id, false);

    let (command, text) = match user_text.find(' ') {
        Some(index) => user_text.split_at(index),
        None => (user_text, "")
    };

    match command {
        "/help" => on_help(&components, chat_id, user.id),
        "/service_message" => on_service_message_command(&components, user.id, text),
        "/start" => on_start(&components, chat_id, user.id, &user.first_name),
        "/statistics" => on_statistics(&components, chat_id, user.id),
        "/stop" => on_stop(&components, user.id),
        "/sources" => on_sources_command(&components, chat_id),
        "/shows" => on_shows_command(&components, chat_id, text),
        "/subscribe" => on_subscribe(&components, chat_id, user.id, text),
        "/subscriptions" => on_subscriptions_command(&components, chat_id, user.id),
        "/unsubscribe" => on_unsubscribe(&components, chat_id, user.id, text),
        _ => warn!("unknown command '{}'", command)
    }
}

fn process_button_click(components: &Components, user: &User, data: &str) {
    info!("user {} clicked at {}", user.id, data);

    match data.parse::<i64>() {
        Ok(show_id) => {
            let ref connection = *components.get_connection();
            match users.select((cid, has_active_session))
                .filter(uid.eq(user.id))
                .first(connection) {
                Ok((chat_id, true)) => create_subscription_at_click(components, connection, chat_id, user.id, show_id),
                Ok((_, false)) => info!("user {} doesn't have active session. skip", user.id),
                Err(error) => error!("Failed to load user's 'has_active_session' field {}: {}", user.id, error),
            }
        },
        Err(error) => error!("Failed on data parsing. '{}' must be string representation of i64: {}", data, error),
    }
}

fn create_subscription_at_click(components: &Components, connection: &PgConnection, chat_id: i64, user_id: i32, show_id: i64) {
    match shows.select(title)
        .filter(sid.eq(show_id))
        .first::<String>(connection) {
        Ok(show_title) => {
            create_subscription(components, connection, chat_id, user_id, show_id, &show_title);
            set_has_active_session(connection, user_id, false);
        },
        Err(error) => error!("Failed to load show {} title: {}", show_id, error)
    }
}

fn print_help(components: &Components, chat_id: i64, is_superuser: bool) {
    let mut help_text =
        "Hi, I'm notifier bot. I can notify you about new episodes of your favorite TV shows.\n\
        \n\
        Available commands:\n".to_string();
    for command in COMMANDS.iter() {
        if is_superuser || command.permission_level == PermissionLevel::User {
            help_text += &command.to_string();
            help_text += "\n"
        }
    }
    components.send_message(chat_id, &help_text);
}

fn on_help(components: &Components, chat_id: i64, user_id: i32) {
    debug!("help command");

    use server::schema::users::dsl::{id, superuser, users};

    let query = users.select(superuser)
        .filter(id.eq(user_id));
    let ref connection = *components.get_connection();
    match query.first(connection) {
        Ok(true) => print_help(components, chat_id, true),
        _ => print_help(components, chat_id, false)
    }
}

fn on_service_message_command(components: &Components, user_id: i32, text: &str) {
    debug!("service message command");

    let ref connection = *components.get_connection();
    do_with_permission(connection, user_id, || {
        info!("service message from user {}: \"{}\"", user_id, text);
        let chat_ids_query = users.select(cid);
        match chat_ids_query.load(connection) {
            Ok(chat_ids) => {
                for chat_id in chat_ids  {
                    components.send_message(chat_id, &text);
                }
            },
            Err(error) => error!("Failed on loading users: {}", error),
        }
    });
}

fn on_start(components: &Components, chat_id: i64, user_id: i32, user_name: &str) {
    debug!("start command");

    use server::models::User;

    print_help(components, chat_id, false);
    let ref connection = *components.get_connection();
    let user = User::new(user_id, user_name.to_string(), chat_id, true, false, false);
    if let Err(error) = insert_into(user_table)
        .values(&user)
        .execute(connection)
        .or_else(|_| update(users.filter(uid.eq(user_id)))
            .set(active.eq(true))
            .execute(connection)) {
        error!("Failed on insert or update user {}: {}", user_id, error)
    }
}

fn on_statistics(components: &Components, chat_id: i64, user_id: i32) {
    debug!("statistics command");

    use diesel::expression::dsl::sql;

    let ref connection = *components.get_connection();
    do_with_permission(connection, user_id, || {
        let users_count = match users.count().get_result::<i64>(connection) {
            Ok(value) => value,
            Err(e) => {
                error!("Failed to load users count: {}", e);
                return;
            }
        };
        let subscriptions_count = match subscriptions.count().get_result::<i64>(connection) {
            Ok(value) => value,
            Err(e) => {
                error!("Failed to load subscriptions count: {}", e);
                return;
            }
        };

        // TODO: use orm operations instead of raw sql
        let query =
            "SELECT
                shows.source_name,
                COUNT(*)
            FROM shows
            GROUP BY shows.source_name;";
        let shows_statistic: Vec<(String, i64)> = match connection.query_by_index(sql(query)) {
            Ok(value) => value,
            Err(e) => {
                error!("Failed to load shows statistic: {}", e);
                return;
            }
        };

        let mut message = format!("\
            users: {}\n\
            subscriptions: {}\n\
            shows:\n", users_count, subscriptions_count);
        for (show, count) in shows_statistic {
            message += &format!("  - {}: {}\n", show, count);
        }

        components.send_message(chat_id, &message);
    })
}

fn on_stop(components: &Components, user_id: i32) {
    debug!("stop command");

    let ref connection = *components.get_connection();
    if let Err(error) = update(users.filter(uid.eq(user_id)))
        .set(active.eq(false))
        .execute(connection) {
        error!("Failed on update user {}: {}", user_id, error)
    }
}

fn on_sources_command(components: &Components, chat_id: i64) {
    debug!("sources command");

    let ref connection = *components.get_connection();
    let query = shows.select(source_name)
        .distinct()
        .order(source_name.asc());
    match query.load::<String>(connection) {
        Ok(source_names) => {
            let all_sources = source_names.iter().join("\n");
            components.send_message(chat_id, &all_sources);
        },
        Err(error) => error!("Failed on sources loading: {}", error),
    }
}

fn on_shows_command(components: &Components, chat_id: i64, text: &str) {
    debug!("shows command");

    let source = text.split_whitespace().next();
    if let Some(source) = source {
        let ref connection = *components.get_connection();
        let query = shows.select(title)
            .filter(source_name.eq(source))
            .order(title.asc());
        match query.load::<String>(connection) {
            Ok(ref titles) if titles.is_empty() => {
                components.send_message(chat_id, "Nothing found");
            },
            Ok(titles) => {
                let max_lines = 100;
                for i in 0 .. (titles.len() + max_lines - 1) / max_lines {
                    let joined_titles = titles[max_lines * i .. min(max_lines * (i + 1), titles.len())].iter().join("\n");
                    components.send_message(chat_id, &joined_titles);
                }
            },
            Err(error) => error!("Failed on shows loading of {}: {}", source, error),
        }
    } else {
        components.send_message(chat_id, "Usage: /shows <source>");
    }
}

fn on_subscribe(components: &Components, chat_id: i64, user_id: i32, text: &str) {
    debug!("subscribe command");

    let mut message_iter = text.split_whitespace();
    let source = message_iter.next();
    let input_title: String = message_iter.join(" ");
    match source {
        Some(source) if !input_title.is_empty() => {
            let title_regex = format!("%{}%", input_title);
            let query = shows.select((title, sid))
                .filter(source_name.eq(source))
                .filter(title.ilike(title_regex));
            let ref connection = *components.get_connection();
            match query.load::<(String, i64)>(connection) {
                Ok(suitable_shows) => match suitable_shows.len() {
                    0 => components.send_message(chat_id, &format!("({}, {}) isn't found", source, input_title)),
                    1 if suitable_shows[0].0.eq_ignore_ascii_case(&input_title) =>
                        create_subscription(components, connection,
                                            chat_id, user_id, suitable_shows[0].1, &suitable_shows[0].0),
                    1...6 => if set_has_active_session(connection, user_id, true) {
                        components.send_message_with_buttons(chat_id, "Maybe you meant:", &suitable_shows[..]);
                    },
                    _ => components.send_message(chat_id, "There are too many suitable results. Try to clarify title name.")
                },
                Err(error) => error!("Failed to update load shows: {}", error)
            }
        },
        _ => components.send_message(chat_id, "Usage: /subscribe <source> <show title>")
    }
}

fn on_subscriptions_command(components: &Components, chat_id: i64, user_id: i32) {
    debug!("subscriptions command");

    let query = sub_table.inner_join(show_table)
        .select((source_name, title))
        .filter(sub_uid.eq(user_id))
        .order((source_name, title));

    let ref connection = *components.get_connection();
    match query.load::<(String, String)>(connection) {
        Ok(subs) => {
            let max_shows_in_message = 100;
            let mut prev_source = "";
            let mut num_shows_in_message = 0;
            let mut message = String::new();
            for &(ref source, ref show) in subs.iter() {
                if source != prev_source {
                    message += "*";
                    message += source;
                    message += ":*\n";
                    prev_source = source
                }
                message += "  - ";
                message += show;
                message += "\n";
                num_shows_in_message += 1;
                if num_shows_in_message == max_shows_in_message {
                    components.send_message(chat_id, &message);
                    num_shows_in_message = 0;
                }
            }
            if num_shows_in_message > 0 {
                components.send_message(chat_id, &message);
            }
        },
        Err(error) => error!("{}", error)
    }
}

fn on_unsubscribe(components: &Components, chat_id: i64, user_id: i32, text: &str) {
    debug!("unsubscribe command");

    let mut message_iter = text.split_whitespace();
    let source = message_iter.next();
    let show_title = message_iter.join(" ");

    match source {
        Some(source) if !show_title.is_empty() => {
            let shows_with_title_and_source = shows
                .select(sid)
                .filter(title.eq(&show_title))
                .filter(source_name.ilike(source));
            let target = subscriptions
                .filter(sub_uid.eq(user_id))
                .filter(sub_sid.eq_any(shows_with_title_and_source));
            let query = diesel::delete(target);
            let ref connection = *components.get_connection();

            match query.execute(connection) {
                Ok(1) => components.send_message(chat_id, &format!("Subscription ({}, {}) removed", source, show_title)),
                Ok(0) => components.send_message(chat_id, &format!("Subscription ({}, {}) not found", source, show_title)),
                res @ _ => error!("{:?}", res),
            }
        },
        _ => components.send_message(chat_id, "Usage: /unsubscribe <source> <show title>")
    }
}

fn create_subscription(components: &Components, connection: &PgConnection,
                       chat_id: i64, user_id: i32, show_id: i64, show_title: &str) {
    let subscription = Subscription::new(show_id, user_id);
    if let Err(error) = insert_into(sub_table).values(&subscription).execute(connection) {
        error!("Failed to create new subscription ({}, {}): {}", show_id, user_id, error);
    } else {
        components.send_message(chat_id, &format!("Subscription to {} created!", show_title));
    }
}

fn set_has_active_session(connection: &PgConnection, user_id: i32, value: bool) -> bool {
    if let Err(error) = update(users.filter(uid.eq(user_id)))
        .set(has_active_session.eq(value))
        .execute(connection) {
        error!("Failed to update 'has_active_session' for user {}: {}", user_id, error);
        false
    } else {
        true
    }
}

fn do_with_permission<F>(connection: &PgConnection, user_id: i32, action: F) -> ()
    where F: Fn() -> () {

    let query = users.select(superuser)
        .filter(uid.eq(user_id));

    match query.first(connection) {
        Ok(true) => action(),
        Ok(false) => info!("user {} isn't superuser. Do nothing", user_id),
        Err(error) => error!("Failed on loading user {}: {}", user_id, error),
    }
}
