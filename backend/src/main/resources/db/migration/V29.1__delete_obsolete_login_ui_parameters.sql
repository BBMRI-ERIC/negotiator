-- Migration to delete obsolete login UI parameters
DELETE FROM ui_parameter WHERE category = 'login' AND name IN (
  'loginNegotiatorTextColor',
  'loginLinksColor',
  'loginLinksTextColor',
  'logincardColor',
  'loginTextColor'
);

-- Delete all parameters with category 'filtersSort'
DELETE FROM ui_parameter WHERE category = 'filtersSort';

-- Delete all parameters with category 'negotiationList'
DELETE FROM ui_parameter WHERE category = 'negotiationList';
