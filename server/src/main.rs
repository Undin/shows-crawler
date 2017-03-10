extern crate diesel;
extern crate dotenv;
extern crate reqwest;
extern crate r2d2;
extern crate r2d2_diesel;
extern crate server;
extern crate threadpool;

use dotenv::dotenv;
use diesel::pg::PgConnection;
use diesel::pg::upsert::*;
use diesel::prelude::*;
use r2d2::{Config, Pool};
use r2d2_diesel::ConnectionManager;
use reqwest::Client;
use server::models::{Show, Source, Subscription};
use server::telegram_api::{Chat, Message, TelegramApi, Update, User};
use std::convert::Into;
use std::str::SplitWhitespace;
use std::env;
use std::sync::Arc;
use threadpool::ThreadPool;

#[derive(Clone)]
struct Components {
    pub api: Arc<TelegramApi>,
    pub connection_pool: Pool<ConnectionManager<PgConnection>>
}

impl Components {
    pub fn new(api: TelegramApi, connection_pool: Pool<ConnectionManager<PgConnection>>) -> Components {
        Components { api: Arc::new(api), connection_pool: connection_pool}
    }
}

fn main() {
    dotenv().ok();
    let bot_token = env::var("BOT_TOKEN").expect("BOT_TOKEN variable must be set");
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");

    let client: Client = reqwest::Client::new().unwrap();
    let components = Components::new(TelegramApi::new(client, bot_token),
                                     create_connection_pool(database_url));
    let thread_pool = ThreadPool::new_with_name("thread-pool".into(), 4);

    let mut update_id = 0i32;

    loop {
        match components.api.get_updates(20, 1, update_id) {
            Ok(update_response) => {
                let updates: Vec<Update> = update_response.result;
                for update in updates {
                    update_id = update.update_id + 1;
                    let components = components.clone();
                    thread_pool.execute(move || process_update(components, update));
                }
            }
            Err(error) => println!("{}", error),
        }
    }
}

fn create_connection_pool<S: Into<String>>(database_url: S) -> Pool<ConnectionManager<PgConnection>> {
    let config = Config::default();
    let connection_manager = ConnectionManager::new(database_url);
    Pool::new(config, connection_manager).expect("Failed to create connection pool")
}

fn process_update(components: Components, update: Update) {
    for message in update.message {
        match message {
            Message { chat: Chat { id: chat_id, .. },
                from: Some(User { id: user_id, first_name }),
                text: Some(user_text), .. } => {
                println!("{}", user_text);
                let mut iter: SplitWhitespace = user_text.split_whitespace();
                let command = iter.next();
                match command {
                    Some("/start") => on_start(&components, chat_id, user_id, first_name),
                    Some("/subscribe") => on_subscribe(&components, chat_id, user_id, &mut iter),
                    Some("/sources") => on_sources_command(&components, chat_id),
                    _ => {
                        println!("unknown command");
                    }
                }
            }
            _ => println!("skip message"),
        }
    }
}

fn on_start(components: &Components, chat_id: i64, user_id: i32, first_name: String) {
    println!("on start");

    use server::models::User;
    use server::schema::users;

    components.api.send_message(chat_id, &format!("Hello, {}!", &first_name));
    let ref connection = *components.connection_pool.get()
        .expect("Unable get connection from connection pool");
    let user = User::new(user_id, first_name);
    diesel::insert(&user.on_conflict_do_nothing())
        .into(users::table)
        .execute(connection)
        .expect("Failed on insert user to db");
}

fn on_subscribe(components: &Components, chat_id: i64, user_id: i32, message_iter: &mut SplitWhitespace) {
    println!("subscribe command");

    use server::schema::shows::dsl::*;
    use server::schema::sources::dsl::*;
    use server::schema::subscriptions;

    let maybe_source = message_iter.next();
    let maybe_show = message_iter.next();
    match (maybe_source, maybe_show) {
        (Some(source_name), Some(show_name)) => {
            let ref connection = *components.connection_pool.get()
                .expect("Unable get connection from connection pool");
            let query_result = sources
                .filter(name.eq(source_name))
                .first::<Source>(connection);
            if let Ok(source) = query_result {
                let query_result = shows
                    .filter(title.eq(show_name).and(source_id.eq(source.id)))
                    .first::<Show>(connection);
                if let Ok(show) = query_result {
                    let subscription = Subscription::new(show.id, user_id);
                    let insertion_result = diesel::insert(&subscription)
                        .into(subscriptions::table)
                        .execute(connection);
                    match insertion_result {
                        Ok(_) => {
                            components.api.send_message(chat_id, &format!("subscription ({}, {}) created!", source_name, show_name));
                        },
                        Err(error) => println!("{}", error)
                    }
                } else {
                    components.api.send_message(chat_id, &format!("show '{}' not found.", show_name));
                }
            } else {
                components.api.send_message(chat_id, &format!("source '{}' not found.", source_name));
            }
        },
        _ => {
            components.api.send_message(chat_id, "Usage: /subscribe <source> <show_name>");
        }
    }
}

fn on_sources_command(components: &Components, chat_id: i64) {
    println!("sources command");

    use server::schema::sources::dsl::*;

    let ref connection = *components.connection_pool.get()
        .expect("Unable get connection from connection pool");

    if let Ok(source_vec) = sources.load::<Source>(connection) {
        let all_sources = source_vec.iter()
            .map(|s| &s.name)
            .fold(String::new(), |mut acc, x| {
                acc.push_str(x);
                acc.push('\n');
                acc
            });
        components.api.send_message(chat_id, &all_sources);
    }
}
