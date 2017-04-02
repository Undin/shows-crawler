extern crate diesel;
extern crate itertools;
#[macro_use]
extern crate log;
extern crate log4rs;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_yaml;
extern crate server;
extern crate threadpool;

use diesel::prelude::*;
use itertools::Itertools;
use server::Components;
use server::models::Subscription;
use server::schema::*;
use server::telegram_api::{Chat, ChatType, Message, Update, User};
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

fn main() {
    log4rs::init_file("log4rs.yaml", Default::default()).unwrap();
    let config = read_config();

    let components = Components::new(config.bot_token, config.database_url);
    let thread_pool = ThreadPool::new_with_name("thread-pool".into(), config.threads);

    let mut update_id = 0i32;

    loop {
        match components.api.get_updates(config.update_timeout, config.update_batch_size, update_id) {
            Ok(update_response) => {
                let updates: Vec<Update> = update_response.result;
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
    for message in update.message {
        match message {
            Message {
                chat: Chat { id: chat_id, chat_type: ChatType::Private },
                from: Some(User { id: user_id, first_name }),
                text: Some(user_text), ..
            } => {
                info!("user {}: {}", user_id, user_text);
                let (command, text) = match user_text.find(' ') {
                    Some(index) => user_text.split_at(index),
                    None => (user_text.as_str(), "")
                };

                match command {
                    "/help" => on_help(&components, chat_id),
                    "/service_message" => on_service_message_command(&components, user_id, text),
                    "/start" => on_start(&components, chat_id, user_id, first_name),
                    "/stop" => on_stop(&components, user_id),
                    "/sources" => on_sources_command(&components, chat_id),
                    "/shows" => on_shows_command(&components, chat_id, text),
                    "/subscribe" => on_subscribe(&components, chat_id, user_id, text),
                    "/subscriptions" => on_subscriptions_command(&components, chat_id, user_id),
                    "/unsubscribe" => on_unsubscribe(&components, chat_id, user_id, text),
                    _ => {
                        warn!("unknown command '{}'", command);
                    }
                }
            }
            _ => debug!("skip message"),
        }
    }
}

fn print_help(components: &Components, chat_id: i64) {
    let help_text =
        "Hi, I'm notifier bot. I can notify you about new episodes of your favorite TV shows.\n\
        \n\
        Available commands:\n\
        /sources - List supported sources\n\
        /shows <source> - List TV shows for particular source. For example, `/shows lostfilm`\n\
        /subscribe <source> <show title> - Create subscription to notifications about TV show. For example, `/subscribe lostfilm Daredevil`\n\
        /unsubscribe <source> <show title> - Remove subscription. For example, `/unsubscribe lostfilm Daredevil`\n\
        /subscriptions - List your current subscriptions";
    components.send_message(chat_id, help_text);
}


fn on_help(components: &Components, chat_id: i64) {
    debug!("help command");

    print_help(components, chat_id);
}

fn on_service_message_command(components: &Components, user_id: i32, text: &str) {
    debug!("service message command");

    use server::schema::users::dsl::*;
    let query = users.select(superuser)
        .filter(id.eq(user_id));

    let ref connection = *components.get_connection();
    match query.first(connection) {
        Ok(true) => {
            info!("service message from user {}: \"{}\"", user_id, text);
            let chat_ids_query = users.select(chat_id);
            match chat_ids_query.load(connection) {
                Ok(chat_ids) => {
                    for cid in chat_ids  {
                        components.send_message(cid, &text);
                    }
                },
                Err(error) => error!("Failed on loading users: {}", error),
            }
        },
        Ok(false) => {
            info!("User {} isn't superuser. Do nothing", user_id)
        },
        Err(error) => error!("Failed on loading user {}: {}", user_id, error),
    }
}

fn on_start(components: &Components, chat_id: i64, user_id: i32, user_name: String) {
    debug!("start command");

    use server::models::User;
    use server::schema::users::dsl::{active, id, users};

    print_help(components, chat_id);
    let ref connection = *components.get_connection();
    let user = User::new(user_id, user_name, chat_id, true, false);
    if let Err(error) = diesel::insert(&user)
        .into(server::schema::users::table)
        .execute(connection)
        .or_else(|_| diesel::update(users.filter(id.eq(user_id)))
            .set(active.eq(true))
            .execute(connection)) {
        error!("Failed on insert or update user {}: {}", user_id, error)
    }
}

fn on_stop(components: &Components, user_id: i32) {
    debug!("stop command");

    use server::schema::users::dsl::*;

    let ref connection = *components.get_connection();
    if let Err(error) = diesel::update(users.filter(id.eq(user_id)))
        .set(active.eq(false))
        .execute(connection) {
        error!("Failed on update user {}: {}", user_id, error)
    }
}

fn on_sources_command(components: &Components, chat_id: i64) {
    debug!("sources command");

    use server::schema::shows::dsl::*;

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

    use server::schema::shows::dsl::*;

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

    use server::schema::shows::dsl::*;

    let mut message_iter = text.split_whitespace();
    let source = message_iter.next();
    let show_title = message_iter.join(" ");
    match source {
        Some(source) if !show_title.is_empty() => {
            let ref connection = *components.get_connection();
            let query = shows.select(id)
                .filter(source_name.eq(source).and(title.eq(&show_title)));
            match query.first::<i64>(connection) {
                Ok(show_id) => {
                    let subscription = Subscription::new(show_id, user_id);
                    let insertion_result = diesel::insert(&subscription)
                        .into(subscriptions::table)
                        .execute(connection);
                    match insertion_result {
                        Ok(_) => {
                            components.send_message(chat_id, &format!("subscription ({}, {}) created!", source, show_title));
                        },
                        Err(error) => error!("{}", error)
                    }
                },
                Err(error) => {
                    components.send_message(chat_id, &format!("({}, {}) isn't found", source, show_title));
                    info!("{}", error);
                }
            }
        },
        _ => {
            components.send_message(chat_id, "Usage: /subscribe <source> <show title>");
        }
    }
}

fn on_subscriptions_command(components: &Components, chat_id: i64, user_id: i32) {
    debug!("subscriptions command");

    use server::schema::shows::dsl::{source_name, title};

    let query = subscriptions::table.inner_join(shows::table)
        .select((source_name, title))
        .filter(subscriptions::user_id.eq(user_id))
        .order((source_name, title).asc());

    let ref connection = *components.get_connection();
    match query.load::<(String, String)>(connection) {
        Ok(subscriptions) => {
            let max_shows_in_message = 100;
            let mut prev_source = "";
            let mut num_shows_in_message = 0;
            let mut message = String::new();
            for &(ref source, ref show) in subscriptions.iter() {
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
        Err(error) => {
            error!("{}", error);
        }
    }
}

fn on_unsubscribe(components: &Components, chat_id: i64, user_id: i32, text: &str) {
    debug!("unsubscribe command");

    let mut message_iter = text.split_whitespace();
    let source_name = message_iter.next();
    let show_title = message_iter.join(" ");

    match source_name {
        Some(source_name) if !show_title.is_empty() => {
            let ref connection = *components.get_connection();
            // TODO: use ORM api instead of raw SQL after diesel implements it
            let command = format!(
                "DELETE FROM subscriptions
                 WHERE (user_id, show_id) IN
                      (SELECT
                         user_id,
                         show_id
                       FROM users
                         INNER JOIN subscriptions ON users.id = subscriptions.user_id
                         INNER JOIN shows ON subscriptions.show_id = shows.id
                       WHERE users.id = {} AND shows.source_name = '{}' AND shows.title = '{}');",
                user_id, source_name, show_title);
            match connection.execute(&command) {
                Ok(1) => {
                    components.send_message(chat_id, &format!("subscription ({}, {}) removed", source_name, show_title));
                },
                Ok(0) => {
                    components.send_message(chat_id, &format!("subscription ({}, {}) not found", source_name, show_title));
                },
                res @ _ => error!("{:?}", res),
            }
        },
        _ => {
            components.send_message(chat_id, "Usage: /unsubscribe <source> <show title>");
        }
    }
}
