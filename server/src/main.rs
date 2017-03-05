extern crate dotenv;
extern crate reqwest;
extern crate server;
extern crate threadpool;

use dotenv::dotenv;
use reqwest::{Client, StatusCode};
use server::telegram_api::{Chat, Message, TelegramApi, Update, UpdateResponse, User};
use std::{env, thread};
use std::sync::Arc;
use threadpool::ThreadPool;

fn main() {
    dotenv().ok();
    let bot_token = env::var("BOT_TOKEN").expect("BOT_TOKEN variable must be set");

    let client: Client = reqwest::Client::new().unwrap();
    let api: Arc<TelegramApi> = Arc::new(TelegramApi::new(client, bot_token));
    let mut update_id = 0i32;

    let thread_pool = ThreadPool::new_with_name("thread-pool".into(), 4);
    loop {
        match api.get_updates(20, 1, update_id) {
            Ok(update_response) => {
                let updates: Vec<Update> = update_response.result;
                for update in updates {
                    update_id = update.update_id + 1;
                    let api_copy = api.clone();
                    thread_pool.execute(move || process_update(update, api_copy));
                }
            }
            Err(error) => println!("{}", error),
        }
    }
}

fn process_update(update: Update, api: Arc<TelegramApi>) {
    for message in update.message {
        match message {
            Message { chat: Chat { id: chat_id, .. }, from: Some(user), text: Some(user_text), .. } => {
                println!("{}", user_text);
                api.send_message(chat_id, "Hello!");
            }
            _ => println!("skip message"),
        }
    }
}
