# use the official Bun image
# see all versions at https://hub.docker.com/r/oven/bun/tags
FROM oven/bun:slim AS base
WORKDIR /usr/src/app

# install dependencies into temp directory
# this will cache them and speed up future builds
# install with --production (exclude devDependencies)
FROM base AS install
RUN mkdir -p /temp
COPY package.json bun.lockb /temp/
RUN cd /temp && bun install --frozen-lockfile --production

# copy production dependencies and source code into final image
FROM base AS release
COPY --from=install /temp/node_modules node_modules
COPY . .

# run the app
USER bun
EXPOSE 3000/tcp
ENTRYPOINT [ "bun", "run", "app.ts" ]