-- Migration to delete obsolete login UI parameters
DELETE FROM ui_parameter WHERE category = 'login' AND name IN (
  'loginNegotiatorTextColor',
  'loginLinksColor',
  'loginLinksTextColor',
  'logincardColor',
  'loginTextColor'
);
