extern crate diesel;
extern crate dotenv;
extern crate itertools;
extern crate server;
extern crate threadpool;

use dotenv::dotenv;
use diesel::pg::upsert::*;
use diesel::prelude::*;
use itertools::Itertools;
use server::Components;
use server::models::{Show, Source, Subscription};
use server::schema::*;
use server::telegram_api::{Chat, Message, Update, User};
use std::convert::Into;
use std::str::SplitWhitespace;
use std::env;
use threadpool::ThreadPool;

fn main() {
    dotenv().ok();
    let bot_token = env::var("BOT_TOKEN").expect("BOT_TOKEN variable must be set");
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");

    let components = Components::new(bot_token, database_url);
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

fn process_update(components: Components, update: Update) {
    for message in update.message {
        match message {
            Message {
                chat: Chat { id: chat_id, .. },
                from: Some(User { id: user_id, first_name }),
                text: Some(user_text), ..
            } => {
                println!("{}", user_text);
                let mut iter: SplitWhitespace = user_text.split_whitespace();
                let command = iter.next();
                match command {
                    Some("/start") => on_start(&components, chat_id, user_id, first_name),
                    Some("/subscribe") => on_subscribe(&components, chat_id, user_id, &mut iter),
                    Some("/sources") => on_sources_command(&components, chat_id),
                    Some("/shows") => on_shows_command(&components, chat_id, &mut iter),
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

    components.api.send_message(chat_id, &format!("Hello, {}!", &first_name));
    let ref connection = *components.connection_pool.get()
        .expect("Unable get connection from connection pool");
    let user = User::new(user_id, first_name, true);
    diesel::insert(&user.on_conflict_do_nothing())
        .into(users::table)
        .execute(connection)
        .expect("Failed on insert user to db");
}

fn on_subscribe(components: &Components, chat_id: i64, user_id: i32, message_iter: &mut SplitWhitespace) {
    println!("subscribe command");

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
                        Err(error) => println!("{}", error)
                    }
                },
                Err(error) => {
                    components.api.send_message(chat_id, &format!("({}, {}) isn't found", source_name, show_title));
                    println!("{}", error);
                }
            }
        },
        _ => {
            components.api.send_message(chat_id, "Usage: /subscribe <source> <show_title>");
        }
    }
}

fn on_sources_command(components: &Components, chat_id: i64) {
    println!("sources command");

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
    println!("sources command");

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
                let all_titles = titles.iter().join("\n");
                components.api.send_message(chat_id, &all_titles);
            },
            Err(error) => println!("{}", error),
        }
    } else {
        components.api.send_message(chat_id, "Usage: /shows <source>");
    }
}
