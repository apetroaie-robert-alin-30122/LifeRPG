package com.example.test2

import com.apollographql.apollo.ApolloClient

object ApolloClientInstance {
    val client = ApolloClient.Builder()
        .serverUrl("http://10.0.2.2:8000/graphql")
        .build()
}