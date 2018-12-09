using FluentAssertions;
using System;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.Tests
{
    public class InvalidMolFileTestFixture
    {
        public Guid UserId { get; } = Guid.NewGuid();
        public Guid BlobId { get; }
        public string Bucket { get; }
        public Guid Id { get; } = Guid.NewGuid();
        public Guid CorrelationId { get; } = Guid.NewGuid();

        public InvalidMolFileTestFixture(ImagingTestHarness harness)
        {
            Bucket = UserId.ToString();
            BlobId = harness.UploadBlob(Bucket, "empty.rxn").Result;
            harness.GenerateImage(Id, BlobId, Bucket, UserId, CorrelationId, 200, 200, "SVG", "image/svg+xml").Wait();
        }
    }

    [Collection("Imaging Test Harness")]
    public class InvalidMolFileTest : ImagingTest, IClassFixture<InvalidMolFileTestFixture>
    {
        private Guid CorrelationId;
        private string Bucket;
        private Guid UserId;
        private Guid Id;

        public InvalidMolFileTest(ImagingTestHarness harness, ITestOutputHelper output, InvalidMolFileTestFixture initFixture) : base(harness, output)
        {
            Id = initFixture.Id;
            CorrelationId = initFixture.CorrelationId;
            Bucket = initFixture.Bucket;
            UserId = initFixture.UserId;
        }

        [Fact]
        public void SvgImageGenetating_InvalidMolFile_ReceivedEventShouldContainValidData()
        {
            var evn = Harness.GetImageGenerationFailedEvent(Id);
            evn.Should().NotBeNull();
            evn.UserId.Should().Be(UserId);
            evn.CorrelationId.Should().Be(CorrelationId);
            evn.Image.Width.Should().Equals(200);
            evn.Image.Height.Should().Equals(200);
            evn.Image.Format.ToLower().Should().BeEquivalentTo("svg");
            evn.Image.MimeType.ToLower().Should().BeEquivalentTo("image/svg+xml");
        }
    }
}
