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
use server::models;
use server::schema::users;
use server::telegram_api::{Chat, Message, TelegramApi, Update, UpdateResponse, User};
use std::convert::Into;
use std::env;
use std::sync::Arc;
use threadpool::ThreadPool;

fn main() {
    dotenv().ok();
    let bot_token = env::var("BOT_TOKEN").expect("BOT_TOKEN variable must be set");
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");

    let client: Client = reqwest::Client::new().unwrap();
    let api: Arc<TelegramApi> = Arc::new(TelegramApi::new(client, bot_token));
    let thread_pool = ThreadPool::new_with_name("thread-pool".into(), 4);
    let connection_pool = create_connection_pool(database_url);

    let mut update_id = 0i32;

    loop {
        match api.get_updates(20, 1, update_id) {
            Ok(update_response) => {
                let updates: Vec<Update> = update_response.result;
                for update in updates {
                    update_id = update.update_id + 1;
                    let api = api.clone();
                    let connection_pool = connection_pool.clone();
                    thread_pool.execute(move || process_update(api, connection_pool, update));
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

fn process_update(api: Arc<TelegramApi>, connection_pool: Pool<ConnectionManager<PgConnection>>, update: Update) {
    for message in update.message {
        match message {
            Message { chat: Chat { id: chat_id, .. },
                from: Some(User { id: user_id, first_name }),
                text: Some(user_text), .. } => {
                println!("{}", user_text);
                api.send_message(chat_id, &format!("Hello, {}!", &first_name));
                if user_text.starts_with("/start") {
                    let ref connection = *connection_pool.get()
                        .expect("Unable get connection from connection pool");
                    let user = models::User { id: user_id, first_name: first_name };
                    diesel::insert(&user.on_conflict_do_nothing())
                        .into(users::table)
                        .execute(connection)
                        .expect("Failed on insert user to db");
                }
            }
            _ => println!("skip message"),
        }
    }
}
