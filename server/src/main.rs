extern crate dotenv;
extern crate reqwest;
extern crate server;

use dotenv::dotenv;
use reqwest::{Client, StatusCode};
use server::entities::{UpdateResponse, Update};
use std::env;

fn main() {
    dotenv().ok();
    let bot_token = env::var("BOT_TOKEN").expect("BOT_TOKEN variable must be set");
    let base_url = format!("https://api.telegram.org/bot{}/getUpdates?timeout=20&limit=1&offset=", bot_token);

    let client: Client = reqwest::Client::new().unwrap();
    let mut update_id = 0i32;

    loop {
        let url = base_url.to_string() + &update_id.to_string();
        println!("--> {}", url);

        match client.get(&url).send() {
            Ok(mut response) => {
                println!("response status: {}", response.status());
                if *response.status() == StatusCode::Ok {
                    if let Ok(update_response) = response.json::<UpdateResponse>() {
                        let updates: Vec<Update> = update_response.result;
                        if updates.len() > 0 {
                            let update = &updates[0];
                            update_id = update.update_id + 1;
                            if let Some(ref message) = update.message {
                                println!("{:?}", message)
                            }
                        } else {
                            println!("no updates");
                        }
                    } else {
                        println!("can't parse response body");
                    }
                }
            },
            Err(error) => println!("{}", error)
        }
    }
}
