using MassTransit;
using Sds.Storage.Blob.Core;
using Serilog;
using Serilog.Events;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.Tests
{
    [CollectionDefinition("Imaging Test Harness")]
    public class OsdrTestCollection : ICollectionFixture<ImagingTestHarness>
    {
    }

    public abstract class ImagingTest
    {
        public ImagingTestHarness Harness { get; }

        protected IBus Bus => Harness.BusControl;
        protected IBlobStorage BlobStorage => Harness.BlobStorage;

        public ImagingTest(ImagingTestHarness fixture, ITestOutputHelper output = null)
        {
            Harness = fixture;

            if (output != null)
            {
                Log.Logger = new LoggerConfiguration()
                    .MinimumLevel.Debug()
                    .WriteTo
                    .TestOutput(output, LogEventLevel.Verbose)
                    .CreateLogger()
                    .ForContext<ImagingTest>();
            }
        }
    }
}
