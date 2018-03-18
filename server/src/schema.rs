table! {
    shows (id) {
        id -> Int8,
        raw_id -> Int8,
        title -> Text,
        local_title -> Nullable<Text>,
        last_season -> Nullable<Int4>,
        last_episode -> Nullable<Int4>,
        source_name -> Text,
        show_url -> Text,
    }
}

table! {
    sources (id) {
        id -> Int8,
        name -> Text,
        url -> Text,
    }
}

table! {
    subscriptions (show_id, user_id) {
        show_id -> Int8,
        user_id -> Int4,
    }
}

table! {
    users (id) {
        id -> Int4,
        first_name -> Text,
        chat_id -> Int8,
        active -> Bool,
        superuser -> Bool,
        has_active_session -> Bool,
    }
}

joinable!(subscriptions -> shows (show_id));
joinable!(subscriptions -> users (user_id));

allow_tables_to_appear_in_same_query!(
    shows,
    sources,
    subscriptions,
    users,
);
