using MassTransit;
using Sds.Imaging.Domain.Commands;
using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace Sds.Imaging.LoadTests
{
    public static class ImagingTestHarnessExtensions
    {
        public static async Task<Guid> UploadResource(this ImagingTestHarness harness, string bucket, string fileName)
        {
            return await UploadFile(harness, bucket, Path.Combine(Directory.GetCurrentDirectory(), "Resources", fileName));
        }

        public static async Task<Guid> UploadFile(this ImagingTestHarness harness, string bucket, string path)
        {
            var source = new FileStream(path, FileMode.Open, FileAccess.Read);
            return await harness.BlobStorage.AddFileAsync(Path.GetFileName(path), source, "application/octet-stream", bucket);
        }

        public static async Task PublishGenerateImage(this ImagingTestHarness harness, Guid id, Guid blobId, string bucket, Guid userId, Guid correlationId, int width, int height, string format = "PNG", string mimeType = "image/png")
        {
            await harness.BusControl.Publish<GenerateImage>(new
            {
                Id = id,
                UserId = userId,
                BlobId = blobId,
                Bucket = bucket,
                CorrelationId = correlationId,
                Image = new Domain.Models.Image()
                {
                    Id = Guid.NewGuid(),
                    Width = width,
                    Height = height,
                    Format = format,
                    MimeType = mimeType
                }
            });
        }

        public static async Task GenerateImage(this ImagingTestHarness harness, Guid id, Guid blobId, string bucket, Guid userId, Guid correlationId, int width, int height, string format = "PNG", string mimeType = "image/png")
        {
            await harness.PublishGenerateImage(id, blobId, bucket, userId, correlationId, width, height, format, mimeType);

            harness.WaitWhileProcessingFinished(correlationId);
        }

        public static void WaitWhileProcessingFinished(this ImagingTestHarness harness, Guid correlationId)
        {
            if (!harness.Received.Select<CorrelatedBy<Guid>>(m => m.Context.Message.CorrelationId == correlationId).Any())
            {
                throw new TimeoutException();
            }
        }
    }
}
