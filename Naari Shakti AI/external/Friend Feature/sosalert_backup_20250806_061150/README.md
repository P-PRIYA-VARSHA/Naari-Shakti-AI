# Legal Help Webpage & AI Chatbot

A comprehensive Android application that provides legal assistance through an interactive AI chatbot, helping users navigate legal issues, generate legal documents, and access filing guidance.

## 🚀 Features

### 📋 Legal Issue Categories
- **Criminal Law**: Assault, theft, fraud, traffic violations
- **Civil Law**: Contracts, property disputes, personal injury
- **Family Law**: Divorce, custody, domestic violence
- **Employment Law**: Workplace harassment, wrongful termination
- **Consumer Law**: Fraud, defective products, debt collection
- **Other Legal Issues**: Custom legal problems

### 🤖 AI Chatbot Features
- **Interactive Q&A**: Gathers detailed information about legal issues
- **Jurisdiction-Specific Questions**: Tailored questions based on legal category
- **Voice Input Support**: Speech-to-text functionality
- **Context-Aware Responses**: Dynamic question flow based on user responses

### 📄 Document Generation
- **Auto-filled Legal Complaints**: Generates appropriate legal documents
- **Category-Specific Templates**: Different formats for different legal issues
- **FIR Drafts**: First Information Report templates for criminal cases
- **Civil Complaints**: Court-ready complaint documents
- **Family Court Petitions**: Family law specific documents
- **Employment Complaints**: Workplace discrimination and harassment complaints
- **Consumer Complaints**: Consumer protection documents

### 📋 Filing Guidance
- **Step-by-Step Instructions**: Clear filing procedures
- **Required Documents**: Comprehensive document checklists
- **Official Portal Links**: Direct links to government websites
- **Emergency Contacts**: Relevant helpline numbers
- **Court Information**: Local court directories and contact details

### 📚 Know Your Rights
- **Category-Specific Rights**: Detailed rights for each legal area
- **Emergency Contacts**: Quick access to relevant helplines
- **Downloadable Guides**: Rights information in PDF format
- **Legal Aid Information**: Free legal assistance contacts

## 🏗️ Architecture

### Activities
1. **MainActivity**: Entry point with "Get Legal Help" button
2. **IssueSelectionActivity**: Legal category selection interface
3. **ChatbotActivity**: AI chatbot with interactive Q&A
4. **KnowYourRightsActivity**: Legal rights information
5. **FilingGuidanceActivity**: Filing procedures and official portals

### Data Flow
```
User selects legal category → 
AI chatbot asks relevant questions → 
Generates legal document draft → 
Provides filing guidance → 
Offers rights information and next steps
```

## 🛠️ Technical Implementation

### Dependencies
- **AndroidX**: Core Android components
- **Material Design**: Modern UI components
- **Firebase**: Database for storing legal complaints
- **Speech Recognition**: Voice input functionality

### Key Components
- **Interactive Chatbot**: Dynamic question flow based on legal category
- **Document Templates**: Pre-formatted legal document generation
- **Firebase Integration**: Secure storage of legal information
- **Voice Input**: Speech-to-text for accessibility
- **Web Integration**: Direct links to official government portals

## 📱 User Workflow

### 1. Legal Help Selection
- User taps "Get Legal Help" on main screen
- Selects appropriate legal category from cards
- Confirms selection and proceeds to chatbot

### 2. Interactive Q&A
- AI chatbot asks jurisdiction-specific questions
- User provides detailed information about their case
- System gathers context and evidence information
- Voice input available for hands-free interaction

### 3. Document Generation
- System generates appropriate legal document draft
- Auto-fills user information and case details
- Provides category-specific legal templates
- Includes proper formatting and legal language

### 4. Filing Guidance
- Step-by-step filing instructions
- Required documents checklist
- Official portal links for online filing
- Emergency contact information

### 5. Rights Information
- Category-specific legal rights
- Emergency helpline numbers
- Downloadable rights guides
- Legal aid contact information

## 🔒 Privacy & Security

- **Local Processing**: Sensitive information processed locally
- **Firebase Security**: Encrypted data storage
- **User Consent**: Clear privacy policy and consent
- **Data Minimization**: Only necessary information collected

## 🌐 Official Portal Integration

### Government Websites
- **National Legal Services Authority**: https://nalsa.gov.in
- **Consumer Protection**: https://consumerhelpline.gov.in
- **Employment**: https://www.eeoc.gov
- **Court Directories**: State-specific court websites
- **Police Portals**: Cybercrime and general police websites

### Emergency Contacts
- **Police**: 100
- **Women Helpline**: 1091
- **Child Helpline**: 1098
- **Domestic Violence**: 181
- **Legal Aid**: 1516

## 📋 Legal Document Types

### Criminal Law
- First Information Report (FIR)
- Police complaint letters
- Evidence documentation

### Civil Law
- Civil complaint drafts
- Property dispute petitions
- Contract dispute documents

### Family Law
- Family court petitions
- Divorce applications
- Custody agreements

### Employment Law
- Employment discrimination complaints
- Workplace harassment reports
- Labor law violation complaints

### Consumer Law
- Consumer complaint letters
- Product defect reports
- Service quality complaints

## 🚀 Getting Started

### Prerequisites
- Android Studio
- Firebase project setup
- Google Services configuration

### Installation
1. Clone the repository
2. Open in Android Studio
3. Configure Firebase project
4. Add google-services.json
5. Build and run

### Configuration
- Update Firebase configuration
- Customize legal templates
- Add local court information
- Configure emergency contacts

## 🤝 Contributing

### Development Guidelines
- Follow Android development best practices
- Maintain accessibility standards
- Test with different legal scenarios
- Update legal templates regularly

### Legal Accuracy
- Regular review of legal templates
- Consultation with legal experts
- Updates based on law changes
- Verification of official portal links

## 📞 Support

For technical support or legal accuracy questions:
- **Email**: support@legalhelpapp.com
- **Legal Hotline**: 1800-LEGAL-AID
- **Documentation**: [Link to detailed docs]

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## ⚖️ Legal Disclaimer

This application provides general legal information and document templates. It is not a substitute for professional legal advice. Users should consult with qualified attorneys for specific legal matters. The developers are not responsible for the accuracy of legal information or the outcome of legal proceedings.

---

**Built with ❤️ for accessible legal assistance** 