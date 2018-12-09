using Sds.Imaging.Domain.Commands;
using Sds.Imaging.Domain.Events;
using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace Sds.Imaging.Tests
{
    public static class ImagingTestHarnessExtensions
    {
        public static async Task<Guid> UploadBlob(this ImagingTestHarness harness, string bucket, string fileName)
        {
            var filePath = Path.Combine(Directory.GetCurrentDirectory(), "Resources", fileName);
            var source = new FileStream(filePath, FileMode.Open);
            return await harness.BlobStorage.AddFileAsync(fileName, source, "application/octet-stream", bucket);
        }

        public static async Task GenerateImage(this ImagingTestHarness harness, Guid id, Guid blobId, string bucket, Guid userId, Guid correlationId, int width, int height, string format = "PNG", string mimeType = "image/png")
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

            if (!harness.Received.Select<ImageGenerated>(m => m.Context.Message.CorrelationId == correlationId).Any() && !harness.Received.Select<ImageGenerationFailed>(m => m.Context.Message.CorrelationId == correlationId).Any())
            {
                throw new TimeoutException();
            }
        }
    }
}
