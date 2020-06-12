# webfleet-domains
![Pull Request to Master](https://github.com/LukeDS-it/webfleet-domains/workflows/Pull%20Request%20to%20Master/badge.svg)

This microservice is part of Webfleet, a new distributed cms.

# Building

To build, run `sbt build`

# Running locally

To run locally, first of all start the `docker-compose.yml` in the `environment` directory,
it will provide all the needed dependencies.

You will then need to set the correct environment variables, and then run `sbt run`

# Running on Docker

A docker container will soon be available.

# Environment

The following are the environment variables that can be set or overridden

|      Variable     |                   Default                   |                                    Description                                          |
|-------------------|---------------------------------------------|-----------------------------------------------------------------------------------------|
| SERVER_PORT       | 8080                                        | HTTP Port where the application is exposed                                              |
| JDBC_DATABASE_URL | jdbc:postgresql://localhost:5432/webfleet   | Full JDBC url for the postgresql database for akka persistence                          |
| DATABASE_USER     | webfleet                                    | Username to connect to the DB                                                           |
| DATABASE_PASS     | password                                    | Password to connect to the DB                                                           |
| AUTH_DOMAIN       |                                             | Domain of Auth0 compliant provider. Used to look for $AUTH_DOMAIN/.well-known/jwks.json |
| AUTH_AUDIENCE     |                                             | Audience to validate the jwt token                                                      |
| AUTH_ISSUER       |                                             | Issuer to validate the jwt token                                                        |
