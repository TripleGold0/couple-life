# Private runtime config

This directory is for local-only settings and runtime values.

Git ignores real private files here, including `application-local.yml`.
Commit only templates such as `*.example.yml` and this README.

Backend startup loads local overrides from either:

- `./private/application-local.yml`
- `../private/application-local.yml`

This supports running Spring Boot from the repository root or from
`couple-life-backend`.
