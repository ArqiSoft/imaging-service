using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.LoadTests
{
    [Collection("Imaging Test Harness")]
    public class BulkOfValidFilesTest : ImagingTest
    {
        public BulkOfValidFilesTest(ImagingTestHarness harness, ITestOutputHelper output) : base(harness, output)
        {
        }

        [Fact]
        public async Task LoadTesting_BulkOfValidFiles_ProcessedSuccessfully()
        {
            var correlations = new List<Guid>();

            string[] files = Directory.GetFiles(@"Resources", "*.*");

            for (var i = 1; i <= 50; i++)
            {
                var bucket = i.ToString();
                var userId = Guid.NewGuid();

                foreach (var file in files)
                {
                    var blobId = await Harness.UploadFile(bucket, file);
                    var correlationId = Guid.NewGuid();

                    await Harness.PublishGenerateImage(Guid.NewGuid(), blobId, bucket, userId, correlationId, 200, 200);

                    correlations.Add(correlationId);
                }
            }

            foreach (var correlationId in correlations)
            {
                Harness.WaitWhileProcessingFinished(correlationId);
            }
        }
    }
}
