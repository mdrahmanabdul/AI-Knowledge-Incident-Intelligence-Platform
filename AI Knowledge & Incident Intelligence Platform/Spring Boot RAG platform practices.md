# Anchor India-resident RAG on Spring AI 2.1

The production default for this platform in November 2026 is **Spring AI 2.1.0 on Spring Boot 4.2 and Java 25**. It calls **Claude Sonnet 5, Opus 5 or Haiku 4.5 through Amazon Bedrock's India-only cross-region profiles**. Retrieval combines lexical and dense search with a **self-hosted cross-encoder reranker**. Authorization is enforced between retrieval and the LLM. Every agent action goes through OAuth-scoped remote MCP servers and a deterministic policy engine, never through the model's judgment. Spring AI 2.1 went GA on 20 November 2026 and now ships modular RAG, LLM-based reranking and filtering, `@Tool` with human confirmation, stateless MCP, a Session API and Micrometer observations. That leaves LangChain4j with one main advantage inside Spring Boot: ready-made reranker adapters.

Data residency shapes the rest of the stack. RBI's 2018 directive requires payment-system data to be stored only in India. Anthropic's first-party API offers only global or US inference. Bedrock's `in.` profiles are the only managed in-country LLM path that could be verified. Embeddings, reranking, guardrails and LLM tracing should therefore default to self-hosted or in-region components.

On retrieval, the strongest evidence supports three techniques: contextual chunk enrichment, hybrid BM25 plus dense search, and reranking. Together they cut retrieval failures by **67%** in Anthropic's tests. On security, the controls have to be architectural, because prompt injection still ranks first in OWASP's 2026 list and no filter stops indirect injection from editable Confluence or Jira content.

Five things are still unresolved. Did MeitY shorten the DPDP deadline to 13 November 2026? Which embedding, rerank and guardrail services Bedrock offers in Mumbai and Hyderabad is unconfirmed. Azure's India model catalogue is unconfirmed. The OpenTelemetry GenAI conventions are still unstable.

## Spring AI 2.1 makes LangChain4j optional inside Spring Boot

Spring AI's 2.x line has replaced the 1.x line for greenfield work. **Spring AI 2.1.0 GA and 2.0.3 GA shipped on 20 November 2026**. 2.1.0 is "built on Spring Boot 4.2", and the release notes tell anyone on milestones or release candidates to upgrade because those lack CVE fixes ([Spring blog](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)). Spring Boot 4.2 requires Java 17 or later, supports up to Java 27, and runs on Spring Framework 7.1 ([Spring Boot docs](https://docs.spring.io/spring-boot/4.2/system-requirements.html)). Teams on Boot 3.x are limited to the **1.1.x and 1.0.x maintenance lines** (1.1.8 and 1.0.9, June 2026) ([Spring blog](https://spring.io/blog/2026/06/12/spring-ai-1-1-8-1-0-9-avaialble-now/)). None of the 2.1 RAG, reranking or session features ship there, so starting on Boot 3 means a migration later.

The 2.0 foundation brought Jackson 3, JSpecify null-safety and immutable options. It also consolidated providers onto vendor SDKs: OpenAI went from three variants to one, Anthropic from two to one, and Google onto the GenAI SDK ([Spring blog, 2.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)). The main programming model is a `ChatClient` that runs every request through an ordered advisor chain. Tool execution now happens in an auto-registered `ToolCallingAdvisor`. `ToolSearchToolCallingAdvisor` exposes tools on demand, so you do not pay for every tool schema on every prompt. `StructuredOutputValidationAdvisor` retries when the output fails schema validation ([Spring blog, 2.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)).

2.1 adds the **Session API, which replaces ChatMemory**; ChatMemory and its advisors are deprecated for removal in 3.0.0. It also adds `GuardrailRefusalException`, a `DecisionModel` API, and a `SemanticCacheAdvisor` that now *requires* a `SemanticCacheScopeResolver` bean ([Spring blog, 2.1 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)). New code should start on the Session API, not ChatMemory.

The RAG surface is now modular. `RetrievalAugmentationAdvisor` chains these stages ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)):

1. Query transformers: rewrite, compression, translation and multi-query expansion.
2. A `VectorStoreDocumentRetriever` whose filter can be static, a per-tenant `Supplier`, or set per request.
3. A joiner.
4. `DocumentPostProcessor` hooks.
5. A `ContextualQueryAugmenter` that **declines to answer when retrieval returns nothing**.

Retrieved documents come back in response metadata under `DOCUMENT_CONTEXT`. That is where you build citations and evaluation datasets ([Spring AI testing docs](https://docs.spring.io/spring-ai/reference/api/testing.html)).

2.1 also ships two LLM-based post-processors ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)):

- **`DecisionDocumentReranker`** asks a yes/no question per document, so it makes one LLM call per candidate.
- **`DecisionDocumentFilter`** screens documents for prompt injection, contradiction and relevance. It **fails open by default**, so a regulated deployment should set `failClosed(true)`.

Some defaults are risky for production. `SearchRequest` defaults to `topK=4` with `similarityThreshold=0.0`, which accepts everything ([Spring AI vector DB docs](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)). Tool loops allow **40 calls per tool and 150 in total** before throwing ([Spring AI tools docs](https://docs.spring.io/spring-ai/reference/api/tools.html)). Set all of these explicitly.

Spring AI still has two gaps that matter here. The docs show no hybrid or reciprocal-rank-fusion retriever; `ConcatenationDocumentJoiner` is the only documented joiner. There is also no first-party Cohere, Voyage or TEI cross-encoder module. You will write both yourself: a custom joiner, and a `DocumentPostProcessor` that calls a rerank REST endpoint.

LangChain4j covers that second gap. It moves fast: **1.23.0 shipped the week of 26 October 2026** ([InfoQ](https://infoq.com/news/2026/11/java-news-roundup-oct26-2026)), and its docs now show **1.24.0-beta34** (exact release date unverified). It ships `*-spring-boot4-starter` artifacts for Boot 4 alongside Boot 3.5 starters ([LangChain4j docs](https://docs.langchain4j.dev/tutorials/spring-boot-integration)). It also has the broadest set of `ScoringModel` reranker adapters in Java: in-process ONNX, Cohere, Jina, Vertex AI Ranking, Voyage, Xinference and watsonx ([LangChain4j scoring docs](https://docs.langchain4j.dev/category/scoring-reranking-models)). Its `@McpClientSupplier` methods can take parameters, which enables per-tenant MCP clients ([DEV Community](https://dev.to/jamilxt/spring-ai-vs-langchain4j-in-october-2026-both-shipped-big-here-is-how-to-pick-3bm0)).

The comparisons agree: "Spring AI is the right default if your application already lives in Spring Boot", and LangChain4j suits non-Spring or heavily reactive applications ([DEV Community](https://dev.to/jamilxt/spring-ai-vs-langchain4j-in-october-2026-both-shipped-big-here-is-how-to-pick-3bm0)). You can mix the two, but it has costs. LangChain4j uses its own 15-second connect and 60-second read HTTP timeouts, which ignore Spring Boot's settings ([LangChain4j docs](https://docs.langchain4j.dev/tutorials/spring-boot-integration)). It also defaults to Jackson 2, while Spring AI 2.x uses Jackson 3. Writing a 50-line rerank post-processor is usually cheaper than taking that dependency.

On the runtime, **target Java 25 LTS**. JDK 27 shipped on 15 September 2026 with structured concurrency still in its **seventh preview** (JEP 533) ([InfoQ](https://infoq.com/news/2026/09/java27-released)). Production code on an LTS should therefore fan out vector search, BM25 and multi-query retrieval with `CompletableFuture` on a virtual-thread executor rather than `StructuredTaskScope`. Spring AI's reranker exposes a `virtualThreads` option for the same purpose ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)). Stream answers as server-sent events from `ChatClient.stream()`. `ToolCallingAdvisor` supports both `.call()` and `.stream()` ([Spring AI tools docs](https://docs.spring.io/spring-ai/reference/api/tools.html)), so Spring MVC on virtual threads is enough; you do not need WebFlux throughout.

| Layer | Recommended default (Nov 2026) | Why |
|---|---|---|
| Framework | Spring AI 2.1.0 + Spring Boot 4.2 + Java 25 | Native RAG, MCP and Session APIs; 1.x is maintenance-only ([Spring](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)) |
| Answer LLM | Claude Sonnet 5 / Opus 5 via Bedrock `in.` profiles | Inference stays in ap-south-1/ap-south-2 ([AWS](https://aws.amazon.com/blogs/machine-learning/amazon-bedrock-expands-claude-model-availability-to-india-cross-region-inference/)) |
| Utility LLM (rewrite, routing, filter) | Haiku 4.5 via `in.` profile | Cheap; same residency boundary |
| Embeddings | Self-hosted BGE-M3 on TEI (1024-d, dense + sparse) | India embedding availability on Bedrock unverified ([HF](https://huggingface.co/BAAI/bge-m3)) |
| Store | Aurora PostgreSQL pgvector 0.8.x (lean) or OpenSearch (BM25 + k-NN + RRF) | See retrieval section |
| Reranker | TEI + bge-reranker behind a custom `DocumentPostProcessor` | Sovereign; Cohere/Voyage India hosting unconfirmed ([TEI](https://github.com/huggingface/text-embeddings-inference)) |
| Authorization | OpenFGA/SpiceDB with coarse metadata pre-filter plus BatchCheck post-filter | Only authorized chunks reach the LLM ([OpenFGA](https://openfga.dev/docs/modeling/agents/rag-authorization)) |
| Tools | Remote MCP servers (2026-07-28 spec) as OAuth resource servers | Audience-bound tokens, no passthrough ([MCP](https://modelcontextprotocol.io/specification/2026-07-28/basic/authorization)) |
| Eval | RAGAS 0.4.x offline in CI; sampled online judging | Reference metric library ([Ragas](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/)) |
| Tracing | Micrometer → OTel; self-hosted Langfuse v4 in India | Prompts are personal data; keep traces in-region ([Langfuse](https://langfuse.com/self-hosting)) |

## RBI's store-only-in-India rule decides the model layer

### Payment data cannot sit in an offshore prompt log

The binding constraint is the RBI circular of **6 April 2018** (DPSS.CO.OD.No 2785/06.08.005/2017-18). It requires that "the entire payment data shall be stored in systems located only in India". That covers customer data, payment-sensitive data, payment credentials and transaction data. It applies to authorised payment system operators and banks, and through them to intermediaries and **third-party vendors**; the FAQ refers explicitly to banks participating in NPCI-operated systems ([RBI FAQ](https://www.rbi.org.in/Scripts/FAQView.aspx?Id=130)). Processing abroad is allowed only if the data is deleted abroad and brought back within **one business day or 24 hours, whichever is earlier**. Compliance is evidenced by a System Audit Report from a CERT-In-empanelled auditor ([RBI FAQ](https://www.rbi.org.in/Scripts/FAQView.aspx?Id=130)).

Incident tickets, logs and chat transcripts often contain VPAs, account numbers, card numbers or customer-linked transaction IDs. If they go to an offshore LLM API that logs or retains prompts, that almost certainly counts as storage abroad. This is an inference: the FAQ predates LLMs and RBI has issued no LLM-specific ruling on it.

This leaves three workable designs:

1. In-country managed inference with zero retention, backed by contract.
2. Self-hosted open-weight models in Indian data centres.
3. Strict tokenisation of payment and personal fields before any external call, with the re-identification map kept in India.

One detail matters for design 1. Bedrock's India Claude offering uses zero data retention, but "certain models require AWS human review if content is flagged" ([AWS ML blog](https://aws.amazon.com/blogs/machine-learning/amazon-bedrock-expands-claude-model-availability-to-india-cross-region-inference/)). Content flagged by OpenAI GPT-5.6 abuse classifiers is retained for offline abuse detection ([AWS ML blog](https://aws.amazon.com/blogs/machine-learning/introducing-openai-models-on-amazon-bedrock-for-in-country-inferencing-in-india/)). Where that review happens and where retained content is stored should be confirmed in the contract before any payment data reaches a prompt. Tokenising first removes the question.

### DPDP's core duties land by May 2027, or may already apply

The **DPDP Rules, 2025** were notified on 13 or 14 November 2025 (Mondaq gives the 13th, PIB the 14th), with an 18-month phased rollout ([PIB](https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/nov/doc20251117695301.pdf); [Mondaq](https://www.mondaq.com/india/data-protection/1708434/operationalising-indias-data-protection-regime-analysing-the-newly-notified-dpdp-rules-2025)). Consent Manager obligations applied from about **12 November 2026**. The core obligations apply from about **12 May 2027** ([Mondaq](https://www.mondaq.com/india/data-protection/1708434/operationalising-indias-data-protection-regime-analysing-the-newly-notified-dpdp-rules-2025)):

- notice and consent
- Data Principal rights
- Rule 6 security safeguards
- Rule 7 breach notification
- Rule 8 retention
- Significant Data Fiduciary (SDF) duties
- Rule 15 cross-border processing

The deadline may be earlier. In early 2026 MeitY proposed shortening the window to 12 months, which would make the deadline **13 November 2026**. It also proposed applying SDF cross-border restrictions immediately, and named finance-sector fiduciaries and AI systems as likely SDFs ([Legal500](https://www.legal500.com/intelligence/india/privacy/meity-plans-to-cut-short-dpdp-compliance-timeline-and-notify-cross-border-restrictions-for-sdfs)). This research found no evidence the amendment was notified. Check the e-Gazette before planning around May 2027.

What the Rules require from the platform is clear either way. The RAG index, prompt logs and audit trail are personal-data processing systems. Rule 6 requires encryption, masking or tokenisation, access controls, and logging and monitoring for unauthorised access. Rule 7 requires a **detailed breach report to the Data Protection Board within 72 hours**. Rule 8(3) requires logs to be kept for **at least one year** ([Mondaq](https://www.mondaq.com/india/data-protection/1708434/operationalising-indias-data-protection-regime-analysing-the-newly-notified-dpdp-rules-2025)). Penalties reach **₹250 crore** for failing to keep reasonable security safeguards ([PIB](https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/nov/doc20251117695301.pdf)). Two practical consequences follow. Erasure requests (90 days per PIB) must propagate into vector indexes and caches, which the deletion pipeline below provides. Log retention needs a floor as well as a ceiling. Sector rules were not researched here and need separate review: RBI outsourcing and IT directions, CERT-In reporting, and RBI's FREE-AI framework.

### Bedrock's India profiles are the verified managed path

Since **29 September 2026**, Claude Opus 5, Sonnet 5 and Haiku 4.5 have been available on Bedrock through India geographic profiles. These "route only between ap-south-1 and ap-south-2" and keep inference in India, and customer data is not stored in the destination region. Example model IDs are `in.anthropic.claude-sonnet-5` and `in.anthropic.claude-haiku-4-5-20251001-v1:0` ([AWS ML blog](https://aws.amazon.com/blogs/machine-learning/amazon-bedrock-expands-claude-model-availability-to-india-cross-region-inference/)). OpenAI's **GPT-5.6 Terra and Luna** followed on 27 August 2026 as `in.openai.*` profiles ([AWS ML blog](https://aws.amazon.com/blogs/machine-learning/introducing-openai-models-on-amazon-bedrock-for-in-country-inferencing-in-india/)). Be careful with the prefix: `global.` profiles route worldwide, and an earlier AWS post on Claude "in India" used global inference ([AWS ML blog](https://aws.amazon.com/blogs/machine-learning/access-anthropic-claude-models-in-india-on-amazon-bedrock-with-global-cross-region-inference)). Enforce the `in.` prefix in configuration validation, not just in team habit.

The alternatives are weaker. Anthropic's own API supports only `global` and `us` for `inference_geo` ([Claude docs](https://platform.claude.com/docs/en/manage-claude/data-residency)). Microsoft opened India South Central (Hyderabad) on 21 September 2026 and positions it for Foundry, but the announcement lists no models ([Microsoft](https://news.microsoft.com/source/asia/2026/09/21/ai-ambition-into-action-microsoft-brings-ai-ready-capabilities-across-its-india-cloud-infrastructure-establishes-india-south-central-region-as-a-strategic-hub-for-asia-and-global-south/)). In Spring AI, Bedrock is accessed through the **Bedrock Converse** chat model, which supports tools and streaming but has no built-in JSON mode ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/chat/comparison.html)). For full sovereignty, Spring AI's OpenAI module can point at a vLLM server through `spring.ai.openai.base-url`, with `extra-body` options for vLLM-specific parameters ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)). That makes self-hosting a configuration change rather than a code change. Keep the `ChatClient` beans provider-agnostic so you can fail over.

### Self-host embeddings and rerankers by default

No source confirmed which embedding or rerank models Bedrock offers in the India regions. Check the [Bedrock region table](https://docs.aws.amazon.com/bedrock/latest/userguide/models-region-compatibility.html) before you commit. Until that is confirmed, the safe default is **BGE-M3 on Hugging Face Text Embeddings Inference (TEI)**. BGE-M3 is a 1024-dimensional, MIT-licensed model covering more than 100 languages with an 8,192-token input. It produces **dense, sparse and multi-vector representations from one model** ([HF](https://huggingface.co/BAAI/bge-m3); [arXiv](https://arxiv.org/abs/2402.03216)), which suits mixed Hindi and English incident text and hybrid retrieval. TEI serves both `/embed` and `/rerank` and supports the bge-reranker and gte-multilingual rerankers ([TEI GitHub](https://github.com/huggingface/text-embeddings-inference)).

Managed alternatives are worth knowing if their regional hosting is confirmed. Cohere embed-v5.0 offers 256 to 2048 Matryoshka dimensions and a 128k context, and its Pro and Fast variants share one embedding space ([Cohere](https://docs.cohere.com/docs/cohere-embed)). OpenAI's text-embedding-3 family can be shortened from 1536 or 3072 dimensions ([OpenAI](https://developers.openai.com/api/docs/guides/embeddings)). Fix the model and dimension early. Spring AI 2.1's new `upsert()` with precomputed vectors ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)) makes later re-embedding manageable, but not cheap.

## Hybrid search, contextual chunks and reranking cut retrieval failures 67%

### The three upgrades with the strongest evidence

Anthropic's Contextual Retrieval study is the clearest available evidence. Retrieval failure was measured as 1 − recall@20. Contextual embeddings alone cut it by **35%** (5.7% to 3.7%). Adding contextual BM25 brought the cut to **49%**. Adding a Cohere reranker over the top 150 candidates, passing the top 20 to the model, brought it to **67%** (1.9%). Top-20 beat top-5 and top-10 ([Anthropic](https://www.anthropic.com/engineering/contextual-retrieval)).

The method prepends 50 to 100 tokens of LLM-written, chunk-specific context to each chunk before both embedding and BM25 indexing. With prompt caching it costs about **$1.02 per million document tokens** ([Anthropic](https://www.anthropic.com/engineering/contextual-retrieval)). A rough extrapolation for a 10-million-chunk corpus is a few thousand dollars, one time. That figure assumes Anthropic first-party pricing, so recompute it for Bedrock India rates.

There is also a Bedrock-specific catch. Haiku 4.5, the natural model for this enrichment, caches only prefixes of **4,096 tokens or more** ([Anthropic caching docs](https://platform.claude.com/docs/en/build-with-claude/prompt-caching)). Short source documents will not benefit from caching at all.

The same study makes a scope point. Knowledge bases **under about 200k tokens** should go straight into the prompt instead of through RAG. Small, curated runbook sets for a single service may not need retrieval at all.

### Lexical search is non-negotiable for error codes and stack traces

Incident corpora are full of exact identifiers: `ERR_5023`, `HTTP 503`, dotted Java class names, Jira keys, service names. Dense embeddings blur these, and BM25 catches them. No study measured hybrid gains on stack traces specifically, so the claim rests on Anthropic's general BM25 result plus established practice. The engine also needs an analyzer that keeps tokens like `ERR-1234` and `com.foo.Bar` intact. Index a keyword or "identifier" subfield, with no stemming, alongside the English text field.

For fusion, the default is reciprocal rank fusion. OpenSearch added it in **2.19** with `rank_constant` 60. OpenSearch's own BEIR tests found RRF averaged **3.86% lower NDCG@10** than tuned score-normalisation hybrid, while being marginally faster ([OpenSearch blog](https://opensearch.org/blog/introducing-reciprocal-rank-fusion-hybrid-search/)). The sensible path is to start with RRF at k=60 and move to weighted or normalised fusion only once a labelled evaluation set exists to tune against. Qdrant's documentation gives the same advice ([Qdrant](https://qdrant.tech/documentation/search/hybrid-queries/)). Note that Qdrant's **default RRF k is 2, not 60**, so set it explicitly ([Qdrant](https://qdrant.tech/documentation/search/hybrid-queries/)).

### Choose the store on filtered, multi-tenant behaviour, not raw QPS

Published benchmarks are almost all vendor-run, and they disagree. Timescale reports pgvectorscale at **471 QPS against Qdrant's 41 QPS at 99% recall on 50M vectors**. The same test shows Qdrant with tighter tail latency (p99 38.7 ms against 74.6 ms), and the authors state "we are, of course, biased towards Postgres" ([Tiger Data](https://www.tigerdata.com/blog/pgvector-vs-qdrant)). Both are fast enough for unfiltered search. What decides the choice is how each store behaves under tenant and ACL filters, the quality of its lexical search, and how well it fits your operations.

**pgvector 0.8.x on Aurora** is the lowest-operations choice for a Spring and JPA team, but it has a filtering weakness. Approximate indexes apply filters *after* the scan. At the default `ef_search` of 40, a filter matching 10% of rows returns about four rows ([pgvector](https://github.com/pgvector/pgvector)). The 0.8 iterative scans fixed completeness: in AWS's test, filtered queries went from 10% or 1% of requested rows to 100%. They improved filtered latency only **1.5 to 1.8 times**, compared with 9.4 times for unfiltered queries ([AWS Database Blog](https://aws.amazon.com/blogs/database/supercharging-vector-search-performance-and-relevance-with-pgvector-0-8-0-on-amazon-aurora-postgresql)). The pgvector README also warns that in a shared index, one tenant's vectors affect recall for the others, and recommends list partitioning per tenant ([pgvector](https://github.com/pgvector/pgvector)). Postgres has no native BM25: `tsvector` ranking is not BM25, so hybrid fusion happens in your Java code.

**OpenSearch or Elasticsearch** give you BM25, k-NN and RRF in one engine. Elasticsearch adds `semantic_text`, with chunking and BBQ quantisation on by default ([Elastic](https://www.elastic.co/search-labs/blog/elasticsearch-semantic-text-vs-opensearch-semantic-field)). **Qdrant** has the most capable multi-stage query API. It supports nested prefetch, DBSF and weighted RRF, and formula-based rescoring for time decay ([Qdrant](https://qdrant.tech/documentation/search/hybrid-queries/)). It also has purpose-built tenancy: `is_tenant` payload indexes, per-tenant HNSW, and tiered promotion of large tenants to dedicated shards. It has one multi-tenant trap. BM25 IDF statistics are shard-wide and blend tenants together unless you use the tenant-scoped `idf` parameter added in v1.19 ([Qdrant multitenancy](https://qdrant.tech/documentation/guides/multiple-partitions/)).

Here is the recommendation. Use Aurora PostgreSQL as the system of record for documents, ACL snapshots and incident metadata. If identifier search is central, which it is for incident intelligence, put retrieval in OpenSearch from the start rather than retrofitting it. A pgvector-only first version, with per-tenant partitions and Java-side RRF, is defensible for smaller corpora. Milvus and Weaviate were not researched and should not be ruled in or out on this evidence. Spring AI supports all of these stores ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)). Hybrid queries, however, will go through `getNativeClient()`, not the portable API.

### Chunk small, by structure, with little overlap

Chroma's chunking study found recursive splitting at **200 to 400 tokens with no overlap** consistently strong: 88 to 89.5% recall. The OpenAI Assistants default of 800 tokens with 400 overlap had below-average recall (85.4%) and the worst precision ([Chroma Research](https://www.trychroma.com/research/evaluating-chunking)). This bears directly on Spring AI. `TokenTextSplitter` defaults to **800-token chunks** ([Spring AI ETL docs](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)), double the top of the best-performing range, so lower it.

Semantic chunkers bought a few recall points at high cost. An independent study concluded that semantic chunking's extra compute "is not justified" by consistent gains ([arXiv 2410.13070](https://arxiv.org/abs/2410.13070)). Late chunking embeds the whole document before pooling per chunk ([arXiv 2409.04701](https://arxiv.org/abs/2409.04701)). It needs a long-context embedding model you control, such as BGE-M3 on TEI, so it is an option only for the self-hosted path.

No study covers chunking for this platform's document types, so the following is practitioner design:

- **Postmortems:** split on template sections (Summary, Impact, Timeline, Root Cause, Remediation, Action Items) and attach the incident ID, severity, services and error signatures as metadata.
- **OpenAPI specs:** one chunk per operation, with `$ref` schemas resolved inline.
- **Runbooks:** one chunk per procedure, using parent-child links so that retrieving one step returns the whole procedure.
- **Code:** split on AST boundaries.
- **Tables:** never split mid-table.

### Rerank 50–150 candidates down to 8–20

The funnel that follows from the evidence:

1. BM25 top 100 and dense top 100.
2. RRF fusion to 50–150 candidates.
3. A cross-encoder reranks them.
4. The top 8–20 go to the LLM.

Every mainstream reranker accepts a query plus a list of documents and returns indices with relevance scores:

- **Cohere** `/v2/rerank`, with up to 1,000 documents, plus `relevant_spans` on v5 for citation highlighting ([Cohere](https://docs.cohere.com/reference/rerank)).
- **Voyage** `rerank-3` and `rerank-3-lite`, with 32k context ([Voyage](https://docs.voyageai.com/docs/reranker)).
- **TEI** `/rerank`, for self-hosted bge-reranker ([TEI](https://github.com/huggingface/text-embeddings-inference)).

Each fits in a short `DocumentPostProcessor` built on `RestClient`. Spring AI's built-in `DecisionDocumentReranker` makes one LLM call per document, so its cost grows linearly with candidate count. Keep it for small final passes. The docs recommend running `DecisionDocumentFilter` *before* the reranker ([Spring AI docs](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)). Run it on the reranked top 20, not all 150 candidates, so the per-document LLM cost stays bounded. No source published reranker latency for 50 to 100 documents, so measure it.

### Graphs, query rewriting and caches are second-order

Microsoft's LazyGraphRAG costs the same to index as vector RAG, which is **0.1% of full GraphRAG**. Its advantage over plain vector RAG is "global" sensemaking questions, while vector RAG handles local who, what and where questions ([Microsoft Research](https://www.microsoft.com/en-us/research/blog/lazygraphrag-setting-a-new-standard-for-quality-and-cost/)). Most incident queries are local. Blast-radius questions such as "incidents on services upstream of auth" are better answered by joining the deterministic service-dependency graph you already have (CMDB, service catalogue, tracing) with incident metadata. An LLM-extracted graph is the worse option there. No primary evidence was gathered on HyDE, multi-query or decomposition. Treat them as optional, evaluation-gated add-ons. Conversational rewriting is close to mandatory for chat, while HyDE can invent identifiers in identifier-heavy queries.

Semantic answer caching needs strict limits. AWS's ElastiCache benchmark, run on generic Chatbot Arena traffic, cut cost **51.9% at a 0.95 similarity threshold**. Cached answers were still only **92.6% accurate** ([AWS ElastiCache docs](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/semantic-caching-benchmarks.html)). An 8% wrong-answer rate is unacceptable for remediation guidance. Spring AI's mandatory `SemanticCacheScopeResolver` is the right hook. Scope keys to tenant, ACL set and corpus version, use short TTLs, and prefer caching retrieval results and embeddings over final answers.

## Authorization and injection defences must live outside the prompt

### Filter by permission between retrieval and generation

OpenFGA's RAG guidance states the rule: authorization "should happen **after** retrieval and **before** the documents reach the LLM". It describes two patterns ([OpenFGA](https://openfga.dev/docs/modeling/agents/rag-authorization)):

- **Pre-filtering:** call `ListObjects` and pass the allowed document IDs into the vector filter. Use it when a user can see few documents or you need an exact top-K.
- **Post-filtering:** run `BatchCheck` on 2–3 times as many candidates as you need. Use it when a user can see most documents.

SpiceDB has equivalent integrations ([AuthZed](https://authzed.com/docs/spicedb/integrations/pinecone)).

The practical design is a hybrid of the two. At ingestion, stamp every chunk with immutable metadata: tenant, source system, space or repo, classification and principal groups. Apply those as a coarse pre-filter through Spring AI's `Supplier`-based `filterExpression`. Then run a final FGA `BatchCheck` in a `DocumentPostProcessor` before the prompt is built. Pass identity through `ToolContext`, which "is never sent to the model" ([Spring AI tools docs](https://docs.spring.io/spring-ai/reference/api/tools.html)). Model group membership as FGA tuples rather than expanding it into chunk metadata, so membership changes need no re-index. Sync Confluence, SharePoint and Git permissions on webhooks, and run a periodic full reconciliation. This sync design is inferred; connector specifics were not researched.

When in doubt, deny. Fail closed when the authorization engine is unavailable ([Paragon](https://www.useparagon.com/blog/rag-freshness-incremental-sync-deduplication)). Never rely on "do not reveal X" system-prompt instructions. OWASP's 2026 list broadened LLM08 into **Hidden Context Exposure** because hidden context leaks ([CSA](https://cloudsecurityalliance.org/articles/the-owasp-top-10-for-llm-applications-2026-what-changed-and-why-it-matters)).

### MCP 2026-07-28 is stateless and strict about tokens

The current MCP revision, **2026-07-28**, makes the protocol stateless. It removes the `initialize` handshake and `Mcp-Session-Id`, adds a mandatory `server/discover`, and replaces server-initiated sampling and elicitation with Multi Round-Trip Requests. It also deprecates the HTTP+SSE transport and OAuth Dynamic Client Registration, the latter in favour of Client ID Metadata Documents ([MCP changelog](https://modelcontextprotocol.io/specification/2026-07-28/changelog)). Spring AI 2.1 supports this revision through MCP Java SDK 2.2.0 ([Spring blog](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)). It provides `@McpTool` annotations, Streamable HTTP as the default transport, and OAuth 2.0 via spring-ai-community/mcp-security ([Spring blog, 2.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)).

The authorization rules are normative ([MCP Authorization](https://modelcontextprotocol.io/specification/2026-07-28/basic/authorization)):

- The MCP server is an OAuth 2.1 resource server and MUST publish RFC 9728 metadata.
- Clients MUST send RFC 8707 `resource` indicators set to the server's canonical URI.
- Clients MUST validate the RFC 9207 `iss` value.
- Servers MUST validate that each token's audience is that server, and "MUST NOT accept or transit any other tokens".
- Insufficient scope triggers a `403 insufficient_scope` step-up challenge.

Token passthrough is "explicitly forbidden". The security guidance also documents confused-deputy attacks through proxy consent, SSRF through OAuth discovery URLs pointing at `169.254.169.254`, and malicious one-click installs of local servers ([MCP Security Best Practices](https://modelcontextprotocol.io/specification/2025-11-25/basic/security_best_practices)).

For this platform, run Jira, Kubernetes, Prometheus and PagerDuty as separate remote MCP servers. Use Spring Security `oauth2ResourceServer` with the audience set to the server's canonical URI. Each server should get its own downstream credential through RFC 8693 token exchange, with the user as `sub` and the agent as `act` ([RFC 8693](https://datatracker.ietf.org/doc/html/rfc8693)). Never forward the user's token. Put egress through an allow-listing proxy. Hash and review `tools/list` output so a changed tool description raises an alert. OWASP lists MCP runtime poisoning under ASI04 Agentic Supply Chain ([OWASP GenAI](https://genai.owasp.org/2025/12/09/owasp-top-10-for-agentic-applications-the-benchmark-for-agentic-security-in-the-age-of-autonomous-ai/)). Because the protocol is now stateless, approval-pending state must live in server-minted handles, not sessions.

### Tier every action, and let policy rather than the model decide

OWASP's 2026 LLM list moved **Excessive Agency from #6 to #3** and **Unbounded Consumption from #10 to #6** ([CSA](https://cloudsecurityalliance.org/articles/the-owasp-top-10-for-llm-applications-2026-what-changed-and-why-it-matters)). That list comes from a secondary source and was not confirmed on owasp.org. The separate Agentic Top 10 names the failure modes an incident agent invites: tool misuse (ASI02), privilege abuse (ASI03), cascading failures (ASI08), and human-agent trust exploitation (ASI09), where a polished explanation persuades an operator to approve something harmful ([OWASP GenAI](https://genai.owasp.org/2025/12/09/owasp-top-10-for-agentic-applications-the-benchmark-for-agentic-security-in-the-age-of-autonomous-ai/)).

No standard defines risk tiers for agent actions, so this scheme is a design proposal:

| Tier | Examples | Gate |
|---|---|---|
| T0 read-only | Prometheus query, log fetch, Jira search | Auto-execute; per-user rate limit |
| T1 low-impact write | Jira comment or ticket | Auto-execute with idempotency key and audit, or one-click confirm |
| T2 operational | Page on-call, scale deployment, restart one pod | Mandatory human approval of rendered parameters plus server-side dry run; approver ≠ requester in prod |
| T3 destructive or broad | Namespace restarts, deletes, config changes | Blocked for agents, or two-person approval plus change ticket |

The mechanics work as follows. The LLM proposes a typed action as JSON. A deterministic executor validates it against an allow-list and the OPA, Cerbos or FGA policy, exchanges a token scoped to that tool with a lifetime of minutes, and executes it. In Spring AI, T2 maps naturally to `@Tool(requiresConfirmation=true)` ([Spring AI tools docs](https://docs.spring.io/spring-ai/reference/api/tools.html)). The policy decision itself must stay outside the model.

Two Spring defaults need changing for an operations agent. Lower the tool-call limits well below 40 and 150. Replace the default exception processor, which returns exception messages to the model and can leak internals.

Approval screens should show raw parameters and diffs, not LLM-written justifications, to counter ASI09. Every proposal and execution goes into an append-only audit record, linked by trace ID through MCP's new `traceparent` in `_meta` ([MCP changelog](https://modelcontextprotocol.io/specification/2026-07-28/changelog)).

### Treat every retrieved chunk as hostile input

Prompt injection remains **LLM01 in 2026**, and Agent Goal Hijack heads the agentic list ([CSA](https://cloudsecurityalliance.org/articles/the-owasp-top-10-for-llm-applications-2026-what-changed-and-why-it-matters); [OWASP GenAI](https://genai.owasp.org/2025/12/09/owasp-top-10-for-agentic-applications-the-benchmark-for-agentic-security-in-the-age-of-autonomous-ai/)). Confluence pages and Jira comments are editable by many people, so each retrieved chunk is an untrusted input that can carry instructions.

The strongest defence is architectural. CaMeL derives control flow only from the trusted user query and attaches capabilities to data, enforcing policy at the moment a tool is called. Untrusted content therefore "cannot change the program's execution path". It solved **77% of AgentDojo tasks with provable security, against 84% undefended** ([arXiv 2503.18813](https://arxiv.org/abs/2503.18813)). That roughly 7-point utility cost is worth paying for T2 and T3 actions.

Several layers sit on top of that architecture:

- **Spotlighting:** delimit and datamark retrieved content, with provenance attributes on each chunk ([arXiv 2403.14720](https://arxiv.org/pdf/2403.14720)).
- **Screening:** run `DecisionDocumentFilter` in fail-closed mode.
- **Output handling** (OWASP LLM10): escape markdown and strip non-allow-listed links and images. Markdown-image exfiltration is the EchoLeak pattern OWASP cites under ASI01.
- **Argument validation:** validate tool arguments against JSON schema.

The rule underneath is simple: **retrieved content never triggers a T2 or T3 tool without a human in the loop**.

### Guardrails at three checkpoints, all in-region

Guardrails belong at three points:

- **Ingestion:** secret scanning and PII redaction before embedding. That means custom recognisers for Aadhaar, PAN, UPI VPAs and Luhn-checked card numbers.
- **Input:** jailbreak and prompt-attack classification.
- **Output:** content safety, a PII re-check, and grounding checks.

Bedrock's `ApplyGuardrail` API works "regardless of the model used". Its **contextual grounding check** scores grounding and relevance against thresholds; AWS examples use 0.75 ([AWS ML Blog](https://aws.amazon.com/blogs/machine-learning/implement-model-independent-safety-measures-with-amazon-bedrock-guardrails/)). Whether Bedrock Guardrails runs in ap-south-1 or ap-south-2 was not verified. Until it is, self-hosted tools such as Presidio and NeMo Guardrails ([NVIDIA](https://developer.nvidia.com/nemo-guardrails)) are the residency-safe choice.

Citation verification also belongs in code. Require the model to cite chunk IDs. Reject any ID that was not in `DOCUMENT_CONTEXT`. Score each cited span against its sentence with a grounding or NLI check. Then refuse, or return "inconclusive", below the threshold.

## Calibrated judges, pinned telemetry and cache-aware prompts keep it operable

### Evaluate retrieval without an LLM, and calibrate the judge that scores generation

RAGAS remains the reference library. Its stable 0.4.x line, with **0.4.3 in January 2026** per a secondary source, deprecates the legacy `ragas.metrics` names ahead of 1.0 ([dev.to](https://dev.to/dublecc/rag-evaluation-metrics-guide-2026-faithfulness-context-precision-and-how-to-score-a-pipeline-46gi)). Its catalogue covers faithfulness, response relevancy, context precision and recall, and noise sensitivity. It also has agent metrics: Tool Call Accuracy, Tool Call F1 and Agent Goal Accuracy ([Ragas docs](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/)). The usual Java setup runs RAGAS in Python in CI against a golden set exported from the app, with retrieved contexts taken from `DOCUMENT_CONTEXT`. Spring AI's `RelevancyEvaluator` and `FactCheckingEvaluator` cover in-JVM smoke tests ([Spring AI testing docs](https://docs.spring.io/spring-ai/reference/api/testing.html)).

Two disciplines matter more than the choice of tool. First, score retrieval separately with Recall@k, MRR and nDCG. These need no judge and catch regressions from chunking or embedding changes cheaply. Resolved incidents make a ready-made golden set: did similar-incident retrieval surface the known duplicate in the top k?

Second, do not trust LLM judges until they are calibrated against **a few hundred hand-labelled answers using Cohen's kappa**. Score both orderings to cancel position bias, and use a judge from a different model family than the generator ([dev.to](https://dev.to/dublecc/rag-evaluation-metrics-guide-2026-faithfulness-context-precision-and-how-to-score-a-pipeline-46gi)). This works well with Bedrock India: generate with Claude and judge with GPT-5.6, both in-country.

Published CI gates such as faithfulness ≥ 0.85 are placeholders. Set floors from your own baseline and ratchet them up ([dev.to](https://dev.to/dublecc/rag-evaluation-metrics-guide-2026-faithfulness-context-precision-and-how-to-score-a-pipeline-46gi)).

### Emit OTel GenAI spans through an adapter, because the names will change

The OpenTelemetry GenAI semantic conventions are **still at Development status**. On 12 June 2026 (v1.42.0) they moved to a separate `semantic-conventions-genai` repository that has no tagged release ([Dash0](https://www.dash0.com/knowledge/opentelemetry-genai-semantic-conventions-explained); [GitHub](https://github.com/open-telemetry/semantic-conventions-genai)). They already define the spans a RAG agent needs: `invoke_agent`, `embeddings`, `retrieval` (with `gen_ai.data_source.id` and `top_k`), `chat` (with usage and cache-read tokens) and `execute_tool`. Prompt content goes into a separate gated event rather than span attributes ([Dash0](https://www.dash0.com/knowledge/opentelemetry-genai-semantic-conventions-explained)). Rerank has no standard operation, so emit a custom span.

Spring AI emits Micrometer observations, including token usage and decision-model calls ([Spring blog](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)). Whether its attribute names match a given semconv version is unverified. Route everything through a thin internal adapter so a rename means one change, and put retrieved document IDs and scores on the retrieval span so evaluation can join traces to sources. Traces contain prompts, and prompts contain personal data, so the backend has to be in India. **Langfuse v4 self-hosts** on PostgreSQL, ClickHouse, Redis and S3 and accepts OpenTelemetry ([Langfuse](https://langfuse.com/self-hosting)). That keeps tracing inside the residency boundary.

### Prompt caching is the largest cost lever, with Bedrock caveats

Anthropic cache reads cost **0.1 times the base input price**, or 0.05 times on the 5.5-generation models. Writes cost 1.25 times for a 5-minute TTL or 2 times for 1 hour, and cache hits do not count against rate limits ([Anthropic caching docs](https://platform.claude.com/docs/en/build-with-claude/prompt-caching)). OpenAI caches automatically. On GPT-5.6 and later, reads cost 0.1 times and writes cost 1.25 times for 30 minutes ([OpenAI](https://developers.openai.com/api/docs/guides/prompt-caching.md)). Retrieved chunks change on every query, so the prompt order should be: tools, system prompt, policy and few-shot examples first, all cached; then retrieved context; then the question. `ToolSearchToolCallingAdvisor` keeps rarely used tool schemas out of that prefix entirely.

Three Bedrock-specific caveats apply:

- **Anthropic's automatic caching is not supported on Bedrock** ([Anthropic caching docs](https://platform.claude.com/docs/en/build-with-claude/prompt-caching)). You need explicit `cache_control` breakpoints, and you should verify they work on the `in.` profiles.
- The models available in India have high minimum prefixes. Sonnet 5 needs **1,024 tokens** and Haiku 4.5 needs **4,096**, and shorter prefixes are silently not cached ([Anthropic caching docs](https://platform.claude.com/docs/en/build-with-claude/prompt-caching)). Cheap utility calls with short prompts get no caching benefit.
- No fetched benchmark validates model routing, but the cost logic is clear. Use Haiku 4.5 for query rewriting, classification, filtering and summaries, and keep Sonnet 5 or Opus 5 for grounded answers and planning. Batch embedding calls during ingestion.

## Incident products converge on evidence-testing agents with post-resolution memory

The commercial market has settled on a common pattern for this product category. **Datadog Bits AI SRE** runs an observe, reason and act loop: it forms root-cause hypotheses, tests them against telemetry, and concludes with either an evidence-backed cause or **"inconclusive"**. It shows the reasoning in a Hypothesis Tree and suggests code, triage or infrastructure fixes. Its default window is the past four hours, and it is reachable through Slack, Teams, MCP, API and CLI ([Datadog docs](https://docs.datadoghq.com/bits_ai/bits_ai_sre/investigate_alerts)).

**PagerDuty's SRE Agent** does the following ([PagerDuty docs](https://docs.pagerduty.com/ai-automation/advance/sre-agent)):

- retrieves similar past incidents and how they were resolved
- finds related active incidents
- checks recent change events
- ingests runbooks from Confluence and GitHub

It keeps **per-service memory that is saved only after resolution**, editable and redactable through a Memory API. It analyses only the first 2,000 characters of each custom field.

incident.io, Rootly and FireHydrant add call transcription, AI-reconstructed timelines and postmortem drafts. Jira Service Management adds alert grouping, change intelligence and AI post-incident reviews ([incident.io](https://incident.io/blog/5-best-ai-powered-incident-management-platforms-2026); [Atlassian](https://www.atlassian.com/software/jira/service-management/aiops)). The reported MTTR gains, such as "up to 80% of incident response" or a 17.8% average reduction, are vendor marketing ([incident.io](https://incident.io/blog/5-best-ai-powered-incident-management-platforms-2026)). Do not use them as targets.

These products imply the feature set users will expect from this platform:

1. Similar-incident retrieval, with the resolution of each match.
2. Alert and active-incident correlation.
3. Change correlation against deploys and PRs.
4. Root-cause hypotheses tested through T0 tools.
5. Runbook suggestions.
6. Generated timelines and postmortem drafts.
7. Per-service memory.
8. Slack, Teams and MCP surfaces.

Three design choices from these products are worth copying:

- **An explicit "inconclusive" outcome.** It refuses to invent a root cause.
- **Writing memory only after resolution, with redaction.** This is the practical defence against memory poisoning (ASI06), because unverified mid-incident guesses never become "learnings".
- **Length limits on ingested fields.** They bound both cost and injection surface.

Two parts of that feature set rest on your own data. Change correlation depends on deploy-event ingestion. Blast-radius reasoning depends on the deterministic service graph discussed earlier.

Ingestion freshness and deletion are what keep these answers trustworthy. Use per-source cursors and **commit writes before advancing the cursor**, because "save it first and a failed write gets silently skipped" ([Paragon](https://www.useparagon.com/blog/rag-freshness-incremental-sync-deduplication)). Build chunk IDs from the source record ID plus a chunk content hash, so only changed chunks are re-embedded. Store the vector IDs belonging to each source record so deletions propagate: hide first, then delete. Run periodic reconciliation for deletions that leave no tombstone. Never deduplicate across tenants ([Paragon](https://www.useparagon.com/blog/rag-freshness-incremental-sync-deduplication)). This is also the mechanism that makes DPDP erasure workable. Freshness targets of minutes for live tickets and hourly for documentation are reasonable starting points ([Paragon](https://www.useparagon.com/blog/rag-freshness-incremental-sync-deduplication)).

At scale, AWS's multi-tenant reference uses shared processing stages with **one queue per tenant**, so that one tenant's failures and retries never block another ([AWS Solutions](https://docs.aws.amazon.com/solutions/multi-tenant-knowledge-base-management-for-scalable-rag-applications-on-aws/)). The Spring Kafka equivalent below is inferred design; no Kafka benchmark was found:

- Topics for source changes, parse and chunk, embed, and upsert or delete, keyed by tenant and document ID.
- A DLQ for each stage.
- Throttling through consumer concurrency plus a token bucket sized to TEI or the provider's limits.
- Partitions paused on HTTP 429.

Isolation should be graded by tenant: a shared index with a server-enforced `tenant_id` filter for internal teams, and a per-tenant namespace or a separate cluster for regulated tenants.

## Conclusion

The common framing treats this as a framework decision: Spring AI or LangChain4j, pgvector or a dedicated store. Two other decisions matter more. The first is **where data is allowed to go**. RBI's store-only-in-India rule, together with Anthropic's lack of an India geography, settles the LLM vendor, embeddings, rerankers, guardrails and even the tracing backend before any benchmark comes into it. The second is **where authority lives**. Authorization filtering, policy-tiered actions, audience-bound MCP tokens and fail-closed screening are all deterministic Java code around the model. This favours a Spring shop, because these are Spring Security, policy-engine and Kafka problems the team already knows how to solve. The parts that are genuinely new — hybrid fusion, cross-encoder reranking and citation verification — are each a few hundred lines on top of Spring AI 2.1's advisor hooks.

The weakest parts of the evidence point to what to do first. Vendor benchmarks conflict, reranker latency is unpublished, and no study measures hybrid gains on stack traces or chunking of postmortems. The highest-value early investment is therefore a **retrieval golden set built from resolved incidents, scored without an LLM**. With it, every later choice becomes a measured decision: OpenSearch versus pgvector, RRF versus weighted fusion, 200- versus 400-token chunks, whether contextual enrichment pays for itself. Three compliance checks should come before any payment-bearing data reaches a model: confirm the DPDP deadline in the e-Gazette, confirm where Bedrock stores human-reviewed flagged content, and confirm Bedrock's India catalogue for embeddings, rerankers and guardrails.
