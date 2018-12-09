FROM microsoft/dotnet:2.1-sdk AS build-env

ARG RID=linux-x64

WORKDIR /build

COPY Sds.Imaging.Domain/Sds.Imaging.Domain.csproj Sds.Imaging.Domain/
COPY Sds.Imaging.Tests/Sds.Imaging.Tests.csproj Sds.Imaging.Tests/
COPY Nuget.config .

RUN dotnet restore --configfile Nuget.config Sds.Imaging.Tests/Sds.Imaging.Tests.csproj

COPY Sds.Imaging.Domain Sds.Imaging.Domain
COPY Sds.Imaging.Tests Sds.Imaging.Tests

RUN dotnet publish Sds.Imaging.Tests/Sds.Imaging.Tests.csproj -r $RID -c Release -o /dist

# Build runtime image
FROM microsoft/dotnet:2.1-sdk

LABEL maintainer="pshenichnov@gmail.com"

WORKDIR /app

RUN apt-get update && apt-get install -y curl
RUN curl https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh > /app/wait-for-it.sh && chmod 777 /app/wait-for-it.sh

COPY --from=build-env /dist ./

ENTRYPOINT ["dotnet", "vstest", "./Sds.Imaging.Tests.dll"]
