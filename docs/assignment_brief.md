
Student Assignment Brief
This document is intended for Coventry University Group students for their own
use in completing their assessed work for this module. It must not be passed to
third parties or posted on any website. If you require this document in an
alternative format, please contact your tutor.
Contents:
Assignment Information
Assignment Overview
Assignment Task
Marking and Feedback
Assessed Module Learning Outcomes
Assignment Support and Academic Integrity
Assessment Marking Criteria
The work you submit for this assignment must be your own independent work, or in
the case of a group assignment your own groups’ work. More information is available
in the ‘Assignment Task’ section of this assignment brief.
Assignment Information
Module Name: Cryptography
Module Code: 500IT
Assignment Title: Designing and Implementing Cryptographic Solutions for
Securing an Online Education Platform: A Case Study of EduSecure
Assignment Due: 18.00 UK Time
Assignment Credits: 30 Credits
Word Count (or equivalent): 2500 words +/- 10%
This document is intended for Coventry University Group students for their own use in completing their

Assignment Type: Report and Artefact
Percentage Grade (Applied Core Assessment). You will be provided with an overall
grade between 0% and 100%. You have one opportunity to pass the assignment at or
above 40%. Resit attempts will be capped at 40%.
Assignment Overview
In today’s cyber landscape, cryptographic controls are the foundation of secure
communications and data protection. This assignment simulates a real-world task where you

serve as the Information Security Officer (ISO) for an online education platform. You will

analyse, design, and implement cryptographic solutions in response to a realistic operational

scenario.

This composite assessment reflects industry-relevant cryptography work where practical

implementation is supported by technical reasoning.

Case Study: EduSecure – Online Education Platform Security
EduSecure is a growing online education platform used by schools and universities to deliver

virtual learning. The system allows:

Students:

Register for courses.
Access learning materials and submit assignments.
Join live virtual classrooms via video.
View exam results and grades.
Lecturers and Admins:

Upload lecture notes and video content.
Record attendance and grade submissions.
Communicate with students.
Manage exam scheduling and feedback forms.
Current Security Issues:

User login credentials are stored in plaintext in the database.
Communication between the web app and users is not encrypted (no HTTPS).
Exam results have been leaked due to SQL injection attacks.
This document is intended for Coventry University Group students for their own use in completing their

Student assignment submissions can be tampered with before reaching lecturers.
The system has no logging or verification for sensitive actions (e.g. grade
changes).
Reported Incidents:

A student intercepted another user’s login token via unsecured campus Wi-Fi.
An attacker modified a submitted assignment file to include malicious content.
Grades were altered in transit, and no integrity check was in place.
Your Challenge:

As the newly appointed Information Security Officer (ISO) for EduSecure , your mission is to

design and implement a secure cryptographic strategy to:

Authenticate users securely (students and lecturers).
Ensure secure file transmission (e.g. assignments and feedback)..
Protect personal records, grades, and login credentials
Provide proof of authorship (digital signature) for assignments.
Prevent tampering and enable auditing for grade changes and sensitive actions.
You are expected to justify all cryptographic selections, simulate part of the system

implementation using standard cryptographic libraries, and demonstrate how your solution

addresses the confidentiality, integrity, and availability (CIA) requirements of the system.

Assignment Task
A. Analyse & Design (Written Report –2,500 words)

Cryptographic Role Analysis (15%)
Explain the role of cryptography in protecting the EduSecure platform.
Discuss key cryptographic primitives: symmetric/asymmetric encryption,
hashes, MACs, and digital signatures.
Analyse benefits, limitations, and practical usage.
Risk Assessment (10%)
Identification of Key Assets: e.g., user credentials, student records, exam results,
assignment submissions.
This document is intended for Coventry University Group students for their own use in completing their

Vulnerability Identification: e.g., plaintext storage of passwords, lack of HTTPS, SQL
injection points.
Threat Modelling: e.g., unauthorised access, data tampering, impersonation,
interception over public Wi-Fi.
Risk Evaluation: Assess the likelihood and impact of each risk. Prioritise risks based
on severity.
Apply a recognised standard such as ISO/IEC 27001/31000 or NIST SP 800- 30 to
structure your analysis.
Academic Enhancement Requirement:

For each entry in your risk register, where a mitigation strategy is proposed, provide a brief

justification using propositional logic to demonstrate formal reasoning.

Secure System Design (20%)
Design secure interaction between students, lecturers, and the system
using UML sequence diagrams.
Show insecure vs. secured versions of communication (e.g. login,
assignment submission, grade retrieval).
Cryptographic Controls & Selection Justification (20%)
Select appropriate cryptographic mechanisms to mitigate the identified
threats.
Compare cryptographic algorithms: AES, RSA, ECC, SHA-256, bcrypt,
HMAC.
Justify algorithm choice (e.g., AES, RSA, SHA- 256 , ECC).
Implementation Plan & Considerations (10%)
Describe how your design will be implemented using existing cryptographic
libraries (e.g., Python’s cryptography, Java, OpenSSL).
Discuss challenges: random number generation, nonce reuse, key
distribution, etc.
Security Evaluation (CIA Model) (10%)
Evaluate how your recommendations ensure Confidentiality , Integrity ,
and Availability.
This document is intended for Coventry University Group students for their own use in completing their

B. Technical Artefact (Code + Output – 15%)

Submit a functional demonstration that implements at least three of the following :

Symmetric encryption (e.g. AES).
Asymmetric encryption (e.g. RSA or ECC).
Hashing or HMAC.
Digital signature creation and verification.
Secure file or message transmission simulation.
The artefact may be:

A working script (Python, Java, etc.).
A CLI tool or Web demo.
Include screenshots if needed. Code must be well-commented and readable.

Submission Instructions:
What do I need to submit?
Technical Report and Artefact: (100 Marks)
Each student is required to submit two components as part of this individual
assignment:
Technical Report
Submit a written technical report (approx. 2500 words) in Microsoft Word format
via the AULA Turnitin submission link. This report should demonstrate your
understanding of the role of cryptography and the methods used to secure
applications and sensitive information, including the use of hashes. Ethical
considerations must be taken into account, and any illegal or unethical activity must
be avoided.
The report must include:
This document is intended for Coventry University Group students for their own use in completing their

A title page with your full name, student number, module code, and
assignment title.
Proper citation of scholarly sources using a consistent referencing style.
Artefact (Code Implementation)
Submit a separate artefact file (e.g., a ZIP file containing your source code) via
the AULA Hand-In submission link. The artefact should clearly demonstrate the
implementation of cryptographic methods discussed in your report.
Important Clarification on File Naming and Submission:
You are required to upload two separate files:
Your technical report (e.g., 1234567_500IT_CW1.docx) via Turnitin.
Your code artefact (e.g., 1234567_500IT_CW1.zip) via Hand-In.
Additional Notes:
This is an individual assignment; group submissions are not permitted. Always
keep backup copies of your work.
Failure to upload successfully will not be accepted as a reason for late or
missed submission.
Marking and Feedback
How will my assignment be marked?
Your assignment will be marked by the module team.
How will I receive my grades and feedback?
Provisional marks will be released once internally moderated.
Feedback will be provided by the module team alongside grades release.
After marking is completed, you can access your marked work and feedback by
clicking on the submission link. Feedback will be provided in the Turnitin viewer and
mark distributions will show you where marks were awarded or deducted. If you are
unsure how to access your feedback, please ask your tutor for clarification.
Your provisional marks and feedback should be available within 2 weeks (10 working
days).
What will I be marked against?
This document is intended for Coventry University Group students for their own use in completing their

Details of the marking criteria for this task can be found at the bottom of this
assignment brief.
Assessed Module Learning Outcomes
The Learning Outcomes for this module align to the marking criteria which can be
found at the end of this brief. Ensure you understand the marking criteria to ensure
successful achievement of the assessment task. The following module learning
outcomes are assessed in this task:
1. Apply propositional logic, sets, functions and relations as modelling tools and
   for reasoning about statements.
2. Evaluate hashing and cryptography methods and use this knowledge in
   selecting appropriate approaches for a given security task.
3. Implement stream and block cipher algorithms, shared and public key
   methods, message authentication codes and digital signatures in cyber
   security contexts.
4. Apply cryptography in software development using existing library
   implementations.