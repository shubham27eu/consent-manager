1. Setup Consent Management	1.1	Set up development environment – Clone the repository, install dependencies, configure environment variables.	
	1.2	Verify system requirements – Ensure compatibility with required frameworks	
	1.3	Run the project locally – Launch backend services, test API endpoints/UI components.	
	1.4	Understand data flow and components – Identify how user input, consent data, and metadata are stored and processed.	
	1.5	Explore data models and database schema – Analyze the structure of stored consents, metadata fields, and audit logs.	
	1.6	Document current functionality – Capture a brief description of core modules and services.	
			
2. Architecture and Design	2.1	Trace end-to-end user workflow – From document ingestion/upload to consent enforcement.	
	2.2	Identify key control flow steps – Authentication, document processing, consent validation, data access.	
	2.3	Map components/services – API layer, processing engine, database, logging, UI (if any).	
	2.4	Use architecture diagram tools – Create a system diagram using Lucidchart, Draw.io, or PlantUML.	
	2.5	Annotate with data flow and responsibilities – Show component interactions, state transitions, and data flows.	
	2.6	Design Document	
			
3. Dataset Preparation	3.1	Define target document types – E.g., payslips, utility bills, invoices, tax forms, medical records.	
	3.2	Search for public datasets or create synthetic samples 	
	3.3	Annotate sample documents	
	3.4	Organize dataset structure – Train/test folders with raw documents and structured annotations.	
	3.5	Preprocess documents – OCR extraction, tokenization, noise filtering, table/section segmentation.	
	3.6	Store metadata – Capture document name, type, source, annotation status, and derived features.	
			
Document Type Modelling 	4.1	Identify common document components – Header, body, footer, tabular sections, totals, identifiers. ( Component )	
	4.2	Abstract document fields into schemas – Define flexible JSON schema models.	
	4.3	Create templates per document type – Invoice schema, bill schema, etc.	
	4.4	LLM or any other models (converter)	
	4.6	Validate structure – Ensure templates are extensible to unseen formats.	
			
5. Document Typing	5.1	Establish privacy definitions for each document type ( Define controlled vs open fields) with user inputs	
	5.2	Tag fields within structure 	
	5.6	Create default policy – Handle unknown fields.	
			
			
7. Integration with Consent	7.1	Design integration point – Connect field classification to consent enforcement.	
	7.2	Update ingestion module – Add pre-processing for classification.	
	7.3	Connect classified fields to consent logic – Match user consent preferences.	
	7.5	Log consent actions – Track violations or masking events.	
	7.6	Test end-to-end flow – Document upload to enforcement.	
	7.7	Document and deploy – API docs, version control, release.	
