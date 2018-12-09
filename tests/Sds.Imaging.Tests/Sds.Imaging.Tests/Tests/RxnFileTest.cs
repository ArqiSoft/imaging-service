using FluentAssertions;
using MassTransit;
using MassTransit.Testing;
using MassTransit.Testing.MessageObservers;
using Sds.Imaging.Domain.Events;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Sds.Imaging.Tests
{
    public class RxnFileTestFixture
    {
        public Guid UserId { get; } = Guid.NewGuid();
        public Guid BlobId { get; }
        public string Bucket { get; }
        public Guid Id { get; } = Guid.NewGuid();
        public Guid CorrelationId { get; } = Guid.NewGuid();

        public RxnFileTestFixture(ImagingTestHarness harness)
        {
            Bucket = UserId.ToString();
            BlobId = harness.UploadBlob(Bucket, "10001.rxn").Result;
            harness.GenerateImage(Id, BlobId, Bucket, UserId, CorrelationId, 200, 200, "SVG", "image/svg+xml").Wait();
        }
    }

    [Collection("Imaging Test Harness")]
    public class RxnFileTest : ImagingTest, IClassFixture<RxnFileTestFixture>
    {
        private Guid CorrelationId;
        private string Bucket;
        private Guid UserId;
        private Guid Id;

        public RxnFileTest(ImagingTestHarness harness, ITestOutputHelper output, RxnFileTestFixture initFixture) : base(harness, output)
        {
            Id = initFixture.Id;
            CorrelationId = initFixture.CorrelationId;
            Bucket = initFixture.Bucket;
            UserId = initFixture.UserId;
        }

        [Fact]
        public async Task MoleculeImageGenetating_ValidMolFile_ShouldGenerateOneImage()
        {
            var evn = Harness.GetImageGeneratedEvent(Id);
            var blobInfo = await Harness.BlobStorage.GetFileInfo(evn.BlobId, Bucket);
            blobInfo.ContentType.Should().BeEquivalentTo("image/svg+xml");
            blobInfo.Length.Should().BeGreaterThan(0);
        }

        [Fact]
        public async Task MoleculeImageGenetating_ValidMolFile_ReceivedEventShouldContainValidData()
        {
            var evn = Harness.GetImageGeneratedEvent(Id);
            evn.Should().NotBeNull();
            evn.BlobId.Should().NotBeEmpty();
            evn.CorrelationId.Should().Be(CorrelationId);
            evn.UserId.Should().Be(UserId);
            evn.Image.Width.Should().Equals(200);
            evn.Image.Height.Should().Equals(200);
            evn.Image.Format.ToLower().Should().BeEquivalentTo("svg");
            evn.Image.MimeType.ToLower().Should().BeEquivalentTo("image/svg+xml");
        }
    }
}
