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
use server::models::{Show, Source, Subscription};
use server::schema::*;
use server::telegram_api::{Chat, ChatType, Message, Update, User};
use std::cmp::min;
use std::convert::Into;
use std::fs::File;
use std::io::BufReader;
use std::str::SplitWhitespace;
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
                let mut iter: SplitWhitespace = user_text.split_whitespace();
                let command = iter.next();
                match command {
                    Some("/start") => on_start(&components, chat_id, user_id, first_name),
                    Some("/stop") => on_stop(&components, user_id),
                    Some("/sources") => on_sources_command(&components, chat_id),
                    Some("/shows") => on_shows_command(&components, chat_id, &mut iter),
                    Some("/subscribe") => on_subscribe(&components, chat_id, user_id, &mut iter),
                    Some("/unsubscribe") => on_unsubscribe(&components, chat_id, user_id, &mut iter),
                    _ => {
                        warn!("unknown command");
                    }
                }
            }
            _ => debug!("skip message"),
        }
    }
}

fn on_start(components: &Components, chat_id: i64, user_id: i32, user_name: String) {
    debug!("on start");

    use server::models::User;
    use server::schema::users::dsl::{active, id, users};

    components.api.send_message(chat_id, &format!("Hello, {}!", &user_name));
    let ref connection = *components.get_connection();
    let user = User::new(user_id, user_name, chat_id, true);
    diesel::insert(&user)
        .into(server::schema::users::table)
        .execute(connection)
        .or_else(|_| diesel::update(users.filter(id.eq(user_id)))
            .set(active.eq(true))
            .execute(connection))
        .expect("Failed on insert or update user");
}

fn on_stop(components: &Components, user_id: i32) {
    debug!("on stop");

    use server::schema::users::dsl::*;

    let ref connection = *components.get_connection();
    diesel::update(users.filter(id.eq(user_id)))
        .set(active.eq(false))
        .execute(connection)
        .expect("Failed on update user");
}

fn on_sources_command(components: &Components, chat_id: i64) {
    debug!("sources command");

    use server::schema::sources::dsl::*;

    let ref connection = *components.get_connection();
    let query = sources.select(name)
        .order(name.asc());
    if let Ok(source_names) = query.load::<String>(connection) {
        let all_sources = source_names.iter().join("\n");
        components.api.send_message(chat_id, &all_sources);
    }
}

fn on_shows_command(components: &Components, chat_id: i64, message_iter: &mut SplitWhitespace) {
    debug!("sources command");

    let source_name = message_iter.next();
    if let Some(source_name) = source_name {
        let ref connection = *components.get_connection();
        let query = sources::table.inner_join(shows::table)
            .select(shows::title)
            .filter(sources::name.eq(source_name))
            .order(shows::title.asc());
        match query.load::<String>(connection) {
            Ok(ref titles) if titles.is_empty() => {
                components.api.send_message(chat_id, "Nothing found");
            },
            Ok(titles) => {
                let max_lines = 100;
                for i in 0 .. (titles.len() + max_lines - 1) / max_lines {
                    let joined_titles = titles[max_lines * i .. min(max_lines * (i + 1), titles.len())].iter().join("\n");
                    components.api.send_message(chat_id, &joined_titles);
                }
            },
            Err(error) => error!("{}", error),
        }
    } else {
        components.api.send_message(chat_id, "Usage: /shows <source>");
    }
}

fn on_subscribe(components: &Components, chat_id: i64, user_id: i32, message_iter: &mut SplitWhitespace) {
    debug!("subscribe command");

    let source_name = message_iter.next();
    let show_title = message_iter.join(" ");
    match source_name {
        Some(source_name) if !show_title.is_empty() => {
            let ref connection = *components.get_connection();
            let query = sources::table.inner_join(shows::table)
                .select(shows::id)
                .filter(sources::name.eq(source_name).and(shows::title.eq(&show_title)));
            match query.first::<i64>(connection) {
                Ok(show_id) => {
                    let subscription = Subscription::new(show_id, user_id);
                    let insertion_result = diesel::insert(&subscription)
                        .into(subscriptions::table)
                        .execute(connection);
                    match insertion_result {
                        Ok(_) => {
                            components.api.send_message(chat_id, &format!("subscription ({}, {}) created!", source_name, show_title));
                        },
                        Err(error) => error!("{}", error)
                    }
                },
                Err(error) => {
                    components.api.send_message(chat_id, &format!("({}, {}) isn't found", source_name, show_title));
                    info!("{}", error);
                }
            }
        },
        _ => {
            components.api.send_message(chat_id, "Usage: /subscribe <source> <show_title>");
        }
    }
}

fn on_unsubscribe(components: &Components, chat_id: i64, user_id: i32, message_iter: &mut SplitWhitespace) {
    debug!("subscribe command");

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
                         INNER JOIN sources ON sources.id = shows.source_id
                       WHERE users.id = {} AND sources.name = '{}' AND shows.title = '{}');",
                user_id, source_name, show_title);
            match connection.execute(&command) {
                Ok(1) => {
                    components.api.send_message(chat_id, &format!("subscription ({}, {}) removed", source_name, show_title));
                },
                Ok(0) => {
                    components.api.send_message(chat_id, &format!("subscription ({}, {}) not found", source_name, show_title));
                },
                res @ _ => error!("{:?}", res),
            }
        },
        _ => {
            components.api.send_message(chat_id, "Usage: /unsubscribe <source> <show_title>");
        }
    }
}
