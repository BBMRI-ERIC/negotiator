-- Correct default URL for footerPrivacyPolicyLink
-- The default was set to the BBMRI-ERIC access policy PDF, which we want to be empty
-- Only update if the value has not been customised (i.e. still matches the original default).
UPDATE ui_parameter
SET value = ''
WHERE category = 'footer'
  AND name = 'footerPrivacyPolicyLink'
  AND value = 'https://www.bbmri-eric.eu/wp-content/uploads/AoM_10_8_Access-Policy_FINAL_EU.pdf';

-- Set navbarPrivacyPolicyLink (ID 71) to the current value of footerPrivacyPolicyLink
-- This respects any customisation: if footerPrivacyPolicyLink was the default (now ''),
-- navbar gets ''; if it was changed to a custom URL, navbar inherits that value
UPDATE ui_parameter
SET value = (
    SELECT value
    FROM ui_parameter
    WHERE category = 'footer'
      AND name = 'footerPrivacyPolicyLink'
)
WHERE category = 'navbar'
  AND name = 'navbarPrivacyPolicyLink';

-- navbarAccessPolicyLink should always be empty — no default URL should be set
UPDATE ui_parameter
SET value = ''
WHERE category = 'navbar'
  AND name = 'navbarAccessPolicyLink';
