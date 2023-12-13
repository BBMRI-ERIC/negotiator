# Contribution guidelines

I'm really glad you are seeing this and are considering contributing to the development of the
Negotiator.
We welcome any pull requests that help us grow as open-source software!

## Development Environment

Our core developers use the [IntelliJ IDEA](https://www.jetbrains.com/idea/) and UNIX based Operating systems.
and can therefore be more helpful in debugging any potential problems in your local development
environment.
For a collection of Postman HTTP requests used to interact with the REST API please contact the development team at
<negotiator@helpdesk.bbmri-eric.eu>.

### Known problems

#### WSL

The current development environment setup is problematic with the WSL.
Make sure your networking configuration is properly adjusted,
especially when using an OIDC provider or a UI client.

## Code style

For maintaining a uniform code style through the code base, please adhere to the
[Google Java Style](https://google.github.io/styleguide/javaguide.html).
To enforce this, we have set up a GitHub action that checks the code style of any modified files
using the [fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin).
You can also set up an IntelliJ IDEA plugin
[google-java-format](https://github.com/google/google-java-format).

## Commit messages

Please follow
the [Conventional Commits specification](https://www.conventionalcommits.org/en/v1.0.0/#summary).

## Submitting changes

Please send us a GitHub Pull Request with a clear description.
We have also provided
a [template](https://github.com/BBMRI-ERIC/negotiator-v3/blob/master/.github/pull_request_template.md)
with a checklist to help you with providing a high-quality contribution.

**Good luck and thank you! 🙇🏻‍♂️** 

