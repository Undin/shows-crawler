extern crate diesel;
extern crate dotenv;
extern crate itertools;
extern crate server;
extern crate threadpool;

use dotenv::dotenv;
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
                    Some("/stop") => on_stop(&components, user_id),
                    Some("/sources") => on_sources_command(&components, chat_id),
                    Some("/shows") => on_shows_command(&components, chat_id, &mut iter),
                    Some("/subscribe") => on_subscribe(&components, chat_id, user_id, &mut iter),
                    Some("/unsubscribe") => on_unsubscribe(&components, chat_id, user_id, &mut iter),
                    _ => {
                        println!("unknown command");
                    }
                }
            }
            _ => println!("skip message"),
        }
    }
}

fn on_start(components: &Components, chat_id: i64, user_id: i32, user_name: String) {
    println!("on start");

    use server::models::User;
    use server::schema::users::dsl::*;

    components.api.send_message(chat_id, &format!("Hello, {}!", &user_name));
    let ref connection = *components.get_connection();
    let user = User::new(user_id, user_name, true);
    diesel::insert(&user)
        .into(server::schema::users::table)
        .execute(connection)
        .or_else(|_| diesel::update(users.filter(id.eq(user_id)))
            .set(active.eq(true))
            .execute(connection))
        .expect("Failed on insert or update user");
}

fn on_stop(components: &Components, user_id: i32) {
    println!("on stop");

    use server::schema::users::dsl::*;

    let ref connection = *components.get_connection();
    diesel::update(users.filter(id.eq(user_id)))
        .set(active.eq(false))
        .execute(connection)
        .expect("Failed on update user");
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

fn on_unsubscribe(components: &Components, chat_id: i64, user_id: i32, message_iter: &mut SplitWhitespace) {
    println!("subscribe command");

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
                res @ _ => println!("{:?}", res),
            }
        },
        _ => {
            components.api.send_message(chat_id, "Usage: /unsubscribe <source> <show_title>");
        }
    }
}
