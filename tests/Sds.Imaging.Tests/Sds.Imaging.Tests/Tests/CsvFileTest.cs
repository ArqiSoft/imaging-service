using FluentAssertions;
using System;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.Tests
{
    public class CsvFileTestFixture
    {
        public Guid UserId { get; } = Guid.NewGuid();
        public Guid BlobId { get; }
        public string Bucket { get; }
        public Guid Id { get; } = Guid.NewGuid();
        public Guid CorrelationId { get; } = Guid.NewGuid();

        public CsvFileTestFixture(ImagingTestHarness harness)
        {
            Bucket = UserId.ToString();
            BlobId = harness.UploadBlob(Bucket, "PropertiesPrediction.csv").Result;
            harness.GenerateImage(Id, BlobId, Bucket, UserId, CorrelationId, 200, 200).Wait();
        }
    }

    [Collection("Imaging Test Harness")]
    public class CsvFileTest : ImagingTest, IClassFixture<CsvFileTestFixture>
    {
        private Guid CorrelationId;
        private string Bucket;
        private Guid UserId;
        private Guid Id;

        public CsvFileTest(ImagingTestHarness harness, ITestOutputHelper output, CsvFileTestFixture initFixture) : base(harness, output)
        {
            Id = initFixture.Id;
            CorrelationId = initFixture.CorrelationId;
            Bucket = initFixture.Bucket;
            UserId = initFixture.UserId;
        }

        [Fact]
        public void PngImageGenetating_ValidMolFile_ReceivedEventShouldContainValidData()
        {
            var evn = Harness.GetImageGenerationFailedEvent(Id);
            evn.Should().NotBeNull();
            evn.CorrelationId.Should().Be(CorrelationId);
            evn.UserId.Should().Be(UserId);
            evn.Image.Width.Should().Equals(200);
            evn.Image.Height.Should().Equals(200);
            evn.Image.Format.ToLower().Should().BeEquivalentTo("png");
            evn.Image.MimeType.ToLower().Should().BeEquivalentTo("image/png");
        }

    }
}
