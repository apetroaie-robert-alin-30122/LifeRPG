package com.example.test2

import com.apollographql.apollo.ApolloClient

object ApolloClientInstance {
    val client = ApolloClient.Builder()
        .serverUrl("http://127.0.0.1:8000//graphql")
        .build()
}