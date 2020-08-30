### How to run Unit tests
1. Create a copy of `gradle-local.properties.example` and name it `gradle-local.properties`
2. Set the value of `project_id`
3. Get the `service-account.json` file associated with the project and place it in the root folder.
4. Run tests with `./gradlew clean test` or `gradlew.bat clean test`