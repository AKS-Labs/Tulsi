# Process for Handling Source Code Requests

This document outlines the internal process for handling source code requests for Tulsi Gallery, as required by the GNU General Public License v3.0 (GPLv3).

## Monitoring Email Requests

1. Check the akslabs.tech@gmail.com email account daily for source code requests
2. Set up email filters to flag messages containing keywords like "source code", "GPL", "license", or "Tulsi"
3. Aim to respond to all requests within 48 hours

## Standard Response Process

When a source code request is received:

1. **Verify the request**: Ensure it's a legitimate request for the Tulsi Gallery source code
2. **Respond with GitHub link**: Send a response that includes:
   - A link to the GitHub repository: https://github.com/AKS-Labs/Tulsi
   - Instructions for cloning the repository: `git clone https://github.com/AKS-Labs/Tulsi.git`
   - A link to the BUILDING.md file for build instructions

### Template Response

```
Subject: Tulsi Gallery Source Code Request

Hello,

Thank you for your interest in the Tulsi Gallery source code. As per the GNU General Public License v3.0 (GPLv3), we're happy to provide access to the complete source code.

You can access the source code in the following ways:

1. GitHub Repository: https://github.com/AKS-Labs/Tulsi
   - Clone the repository using: git clone https://github.com/AKS-Labs/Tulsi.git

2. Build Instructions:
   - Detailed build instructions are available in the BUILDING.md file in the repository
   - Or view them directly at: https://github.com/AKS-Labs/Tulsi/blob/main/BUILDING.md

If you have any difficulties accessing or building the source code, please don't hesitate to reply to this email.

Best regards,
AKS-Labs Team
```

## Alternative Methods (if needed)

If the requester cannot access GitHub or has specific needs:

1. **ZIP file option**: Offer to send a ZIP file of the current source code
   - Create the ZIP file using `git archive --format=zip --output=Tulsi-Source.zip main`
   - Ensure the ZIP file includes the LICENSE.md and all required files
   - Use a file sharing service if the ZIP file is too large for email

2. **Specific version requests**: If a user requests the source code for a specific version:
   - Use `git archive --format=zip --output=Tulsi-vX.X.X.zip vX.X.X` where vX.X.X is the version tag
   - If the version isn't tagged, use the closest commit to the release date

## Record Keeping

Maintain a simple log of source code requests that includes:
1. Date of request
2. Requester email (for follow-up if needed)
3. Date of response
4. Method of fulfillment (GitHub link, ZIP file, etc.)
5. Any follow-up actions required

This log helps ensure compliance with the GPLv3 and improves our response process over time.
