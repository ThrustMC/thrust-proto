.PHONY: lint breaking-pr breaking-release generate clean tag ci-publish-java

GO ?= go

lint:
	buf lint

breaking-pr:
	buf breaking --against '.git#branch=main'

breaking-release:
	buf breaking --against ".git#tag=$$(git describe --tags --abbrev=0)"

generate:
	buf generate
	$(GO) build ./...

clean:
	rm -rf gen build

tag:
	@[ -n "$(VERSION)" ] || { echo "VERSION required"; exit 1; }
	$(MAKE) lint breaking-release
	git tag -a v$(VERSION) -m "v$(VERSION)"

ci-publish-java:
	@[ -n "$(VERSION)" ] || { echo "VERSION required"; exit 1; }
	@[ -n "$(CI)" ] || { echo "CI only"; exit 1; }
	$(MAKE) generate
	./gradlew publish -Pversion=$(VERSION)
