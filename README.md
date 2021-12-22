# ReSenderBot
Bot to resend pictures from a group to a Telegram channel. It adds reaction buttons to the pictures so people can vote. It also supports a few commands like /best and /worst to gather stats on the most liked and less liked photos

## Disclaimer
This bot was done 6 years ago and it was closed source.
The code quality is not good enough, it's not even in full English. I feel ashamed to even have this published at all. But I've always thought that it's better to have something bad publicly available that not being available at all so here it is.

## Getting Started

### Prerequisites

- Java
- Maven

### Use
1. First you should complie the bot with `mvn package`
2. You should set the env vars
  1. authNum: The number of chats athourized to ask for stats
  2. auth"0-n-1"(auth0, auth1): The ids of the authorized chats
  3. chatFrom: Chat from where to grab the images
  4. chatTo: Where to send the images
  5. DATABASE_URL: Including user, the db may not work with passwords or os-level auth (not tested)
  6. prod: true? I don't remember what did this affect, it obviously stands for production, but no clue what did it do.
  7. publishUpdatesOn: Where to post when an image has become the most liked
  8. Token: Telegram token
3. Launch it
``` bash
java -jar ./target/bot.jar
```
## Contributing
This project is abandoned. PRs may be reviewed and accpeted if they fix funtionality. No new funtionality will be added.
