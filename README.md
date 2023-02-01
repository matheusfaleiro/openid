# OAuth Reference App for Android

This is a sample Android project for OpenID Connect/OAuth that utilizes an MVI architecture and RxKotlin.

## Major Dependencies
- RxKotlin/RxAndroid v3
- OpenID AppAuth SDK (https://github.com/openid/AppAuth-Android)
- ConstraintLayout

## Architecture
- MVI
- Core classes (& types)
    - Views
    - Presenters
    - (Auth)Interactor
    - Repository w/ (Auth)State
    - OAuthClient

## Future Improvements
- Dependency injection
- Variants with other tech stacks
- Unit tests
- Working endpoint for testing
