using MassTransit;
using MassTransit.ExtensionsDependencyInjectionIntegration;
using MassTransit.RabbitMqTransport;
using MassTransit.Scoping;
using MassTransit.Testing.MessageObservers;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using MongoDB.Driver;
using Sds.Imaging.Domain.Events;
using Sds.MassTransit.RabbitMq;
using Sds.Storage.Blob.Core;
using Sds.Storage.Blob.GridFs;
using Serilog;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Sds.Imaging.Tests
{
    public class ImagingTestHarness : IDisposable
    {
        protected IServiceProvider _serviceProvider;

        public IBlobStorage BlobStorage { get { return _serviceProvider.GetService<IBlobStorage>(); } }
       
        public IBusControl BusControl { get { return _serviceProvider.GetService<IBusControl>(); } }

        private List<ExceptionInfo> Faults = new List<ExceptionInfo>();

        public ReceivedMessageList Received { get; } = new ReceivedMessageList(TimeSpan.FromSeconds(20));

        public ImagingTestHarness()
        {
            var configuration = new ConfigurationBuilder()
                 .AddJsonFile("appsettings.json", true, true)
                 .AddEnvironmentVariables()
                 .Build();

            Log.Logger = new LoggerConfiguration()
                .CreateLogger();

            Log.Information("Staring Imaging tests");

            var services = new ServiceCollection();

            services.AddTransient<IBlobStorage, GridFsStorage>(x =>
            {
                var gridFsConnectionUrl = new MongoUrl(Environment.ExpandEnvironmentVariables(configuration["GridFs:ConnectionString"]));
                var client = new MongoClient(gridFsConnectionUrl);

                return new GridFsStorage(client.GetDatabase(gridFsConnectionUrl.DatabaseName));
            });

            services.AddSingleton<IConsumerScopeProvider, DependencyInjectionConsumerScopeProvider>();

            services.AddSingleton(container => Bus.Factory.CreateUsingRabbitMq(x =>
            {
                IRabbitMqHost host = x.Host(new Uri(Environment.ExpandEnvironmentVariables(configuration["MassTransit:ConnectionString"])), h => { });

                x.RegisterConsumers(host, container, e =>
                {
                    e.UseInMemoryOutbox();
                });

                x.ReceiveEndpoint(host, "processing_fault_queue", e =>
                {
                    e.Handler<Fault>(async context =>
                    {
                        Faults.AddRange(context.Message.Exceptions.Where(ex => !ex.ExceptionType.Equals("System.InvalidOperationException")));

                        await Task.CompletedTask;
                    });
                });

                x.ReceiveEndpoint(host, "processing_update_queue", e =>
                {
                    e.Handler<ImageGenerated>(context => { Received.Add(context); return Task.CompletedTask; });
                    e.Handler<ImageGenerationFailed>(context => { Received.Add(context); return Task.CompletedTask; });
                });
            }));

            _serviceProvider = services.BuildServiceProvider();

            var busControl = _serviceProvider.GetRequiredService<IBusControl>();

            busControl.Start();
        }

        public ImageGenerated GetImageGeneratedEvent(Guid id)
        {
            return Received
                .ToList()
                .Where(m => m.Context.GetType().IsGenericType && m.Context.GetType().GetGenericArguments()[0] == typeof(ImageGenerated))
                .Select(m => (m.Context as ConsumeContext<ImageGenerated>).Message)
                .Where(m => m.Id == id).ToList().SingleOrDefault();
        }

        public ImageGenerationFailed GetImageGenerationFailedEvent(Guid id)
        {
            return Received
                .ToList()
                .Where(m => m.Context.GetType().IsGenericType && m.Context.GetType().GetGenericArguments()[0] == typeof(ImageGenerationFailed))
                .Select(m => (m.Context as ConsumeContext<ImageGenerationFailed>).Message)
                .Where(m => m.Id == id).ToList().SingleOrDefault();
        }

        public virtual void Dispose()
        {
            var busControl = _serviceProvider.GetRequiredService<IBusControl>();
            busControl.Stop();
        }
    }
}