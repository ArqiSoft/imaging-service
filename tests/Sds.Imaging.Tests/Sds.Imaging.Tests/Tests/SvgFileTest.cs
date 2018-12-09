using FluentAssertions;
using System;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.Tests
{
    public class SvgFileTestFixture
    {
        public Guid UserId { get; } = Guid.NewGuid();
        public Guid BlobId { get; }
        public string Bucket { get; }
        public Guid Id { get; } = Guid.NewGuid();
        public Guid CorrelationId { get; } = Guid.NewGuid();

        public SvgFileTestFixture(ImagingTestHarness harness)
        {
            Bucket = UserId.ToString();
            BlobId = harness.UploadBlob(Bucket, "Aspirin.mol.svg").Result;
            harness.GenerateImage(Id, BlobId, Bucket, UserId, CorrelationId, 200, 200).Wait();
        }
    }

    [Collection("Imaging Test Harness")]
    public class SvgFileTest : ImagingTest, IClassFixture<SvgFileTestFixture>
    {
        private Guid CorrelationId;
        private string Bucket;
        private Guid UserId;
        private Guid Id;

        public SvgFileTest(ImagingTestHarness harness, ITestOutputHelper output, SvgFileTestFixture initFixture) : base(harness, output)
        {
            Id = initFixture.Id;
            CorrelationId = initFixture.CorrelationId;
            Bucket = initFixture.Bucket;
            UserId = initFixture.UserId;
        }

        [Fact]
        public async Task PngImageGenetating_ValidFile_ShouldGenerateOneImage()
        {
            var evn = Harness.GetImageGeneratedEvent(Id);
            var blobInfo = await Harness.BlobStorage.GetFileInfo(evn.BlobId, Bucket);
            blobInfo.ContentType.Should().BeEquivalentTo("image/png");
            blobInfo.Length.Should().BeGreaterThan(0);
        }

        [Fact]
        public void PngImageGenetating_ValidMolFile_ReceivedEventShouldContainValidData()
        {
            var evn = Harness.GetImageGeneratedEvent(Id);
            evn.Should().NotBeNull();
            evn.BlobId.Should().NotBeEmpty();
            evn.UserId.Should().Be(UserId);
            evn.Image.Width.Should().Equals(200);
            evn.Image.Height.Should().Equals(200);
            evn.Image.Format.ToLower().Should().BeEquivalentTo("png");
            evn.Image.MimeType.ToLower().Should().BeEquivalentTo("image/png");
        }
    }
}
