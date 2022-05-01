# LEVEL 3

Welcome to your new Function project!

This sample project contains a single function based on Spring Cloud Function: `Level3Application.uppercase()`, which returns the uppercase of the data passed.

## Usage

```shell
$ http http://test.default.34.116.142.221.sslip.io sessionId="game-blahblah" question1="Awesome"

HTTP/1.1 200 OK
Content-Length: 98
Content-Type: application/json
accept-encoding: gzip, deflate
connection: keep-alive
uri: http://localhost:8080/
user-agent: HTTPie/3.1.0

{
    "level": "level-3",
    "levelScore": 10,
    "sessionId": "game-blahblah",
    "time": "2022-04-19T11:40:46.04108"
}
```

## Local execution

Make sure that `Java 17 SDK` is installed.

To start server locally run `./gradlew bootRun`.
The command starts http server and automatically watches for changes of source code.
If source code changes the change will be propagated to running server. It also opens debugging port `5005`
so a debugger can be attached if needed.

To run tests locally run `./gradlew test`.

## The `func` CLI

It's recommended to set `FUNC_REGISTRY` environment variable.

```shell script
# replace ~/.bashrc by your shell rc file
# replace docker.io/johndoe with your registry
export FUNC_REGISTRY=docker.io/johndoe
echo "export FUNC_REGISTRY=docker.io/johndoe" >> ~/.bashrc
```

### Building

This command builds an OCI image for the function. By default, this will build a GraalVM native image.

```shell script
func build -v                  # build native image
```

**Note**: If you want to disable the native build, you need to edit the `func.yaml` file and
remove (or set to false) the following BuilderEnv variable:
```
buildEnvs:
  - name: BP_NATIVE_IMAGE
    value: "true"
```

### Running

This command runs the func locally in a container
using the image created above.

```shell script
func run
```

### Deploying

This command will build and deploy the function into cluster.

```shell script
func deploy -v # also triggers build
```

## Function invocation

For the examples below, please be sure to set the `URL` variable to the route of your function.

You get the route by following command.

```shell script
func info
```

Note the value of **Routes:** from the output, set `$URL` to its value.

__TIP__:

If you use `kn` then you can set the url by:

```shell script
# kn service describe <function name> and show route url
export URL=$(kn service describe $(basename $PWD) -ourl)
```

### cURL

```shell script
curl -v "$URL/uppercase" \
  -H "Content-Type:text/plain" \
  -d "$(whoami)"
```

### HTTPie

```shell script
echo "$(whoami)" | http -v "$URL/uppercase"
```

## Cleanup

To clean the deployed function run:

```shell
func delete
```
