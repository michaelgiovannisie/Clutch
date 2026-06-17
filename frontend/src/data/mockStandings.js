function row(slug, name, abbr, won, drawn, lost, gf, ga) {
  return { slug, name, abbr, played: won + drawn + lost, won, drawn, lost, gf, ga, gd: gf - ga, points: won * 3 + drawn };
}

export const groups = [
  { id: 'A', teams: [
    row('united-states', 'United States', 'USA', 2, 0, 0, 5, 1),
    row('uruguay',       'Uruguay',       'URU', 1, 1, 0, 3, 2),
    row('panama',        'Panama',        'PAN', 0, 1, 1, 1, 3),
    row('curacao',       'Curaçao',       'CUR', 0, 0, 2, 1, 5),
  ]},
  { id: 'B', teams: [
    row('canada',      'Canada',      'CAN', 1, 1, 0, 4, 2),
    row('colombia',    'Colombia',    'COL', 1, 1, 0, 3, 2),
    row('south-korea', 'South Korea', 'KOR', 0, 1, 1, 2, 3),
    row('ivory-coast', 'Ivory Coast', 'CIV', 0, 1, 1, 1, 3),
  ]},
  { id: 'C', teams: [
    row('mexico',      'Mexico',      'MEX', 2, 0, 0, 4, 1),
    row('brazil',      'Brazil',      'BRA', 1, 0, 1, 3, 2),
    row('iraq',        'Iraq',        'IRQ', 1, 0, 1, 2, 3),
    row('new-zealand', 'New Zealand', 'NZL', 0, 0, 2, 0, 4),
  ]},
  { id: 'D', teams: [
    row('argentina', 'Argentina', 'ARG', 2, 0, 0, 6, 1),
    row('japan',     'Japan',     'JPN', 1, 0, 1, 3, 3),
    row('australia', 'Australia', 'AUS', 1, 0, 1, 2, 3),
    row('algeria',   'Algeria',   'ALG', 0, 0, 2, 1, 5),
  ]},
  { id: 'E', teams: [
    row('england',      'England',      'ENG', 2, 0, 0, 5, 0),
    row('ecuador',      'Ecuador',      'ECU', 1, 0, 1, 3, 3),
    row('south-africa', 'South Africa', 'RSA', 1, 0, 1, 2, 3),
    row('saudi-arabia', 'Saudi Arabia', 'KSA', 0, 0, 2, 0, 4),
  ]},
  { id: 'F', teams: [
    row('france',   'France',   'FRA', 2, 0, 0, 7, 1),
    row('morocco',  'Morocco',  'MAR', 1, 1, 0, 3, 2),
    row('paraguay', 'Paraguay', 'PAR', 0, 1, 1, 2, 3),
    row('ghana',    'Ghana',    'GHA', 0, 0, 2, 1, 8),
  ]},
  { id: 'G', teams: [
    row('germany',    'Germany',    'GER', 2, 0, 0, 5, 1),
    row('egypt',      'Egypt',      'EGY', 1, 0, 1, 2, 3),
    row('uzbekistan', 'Uzbekistan', 'UZB', 1, 0, 1, 2, 3),
    row('cape-verde', 'Cape Verde', 'CPV', 0, 0, 2, 1, 4),
  ]},
  { id: 'H', teams: [
    row('spain',    'Spain',    'ESP', 2, 0, 0, 5, 0),
    row('dr-congo', 'DR Congo', 'COD', 1, 0, 1, 2, 2),
    row('tunisia',  'Tunisia',  'TUN', 1, 0, 1, 2, 3),
    row('haiti',    'Haiti',    'HAI', 0, 0, 2, 0, 4),
  ]},
  { id: 'I', teams: [
    row('portugal', 'Portugal', 'POR', 2, 0, 0, 6, 1),
    row('senegal',  'Senegal',  'SEN', 1, 0, 1, 3, 3),
    row('norway',   'Norway',   'NOR', 1, 0, 1, 2, 3),
    row('jordan',   'Jordan',   'JOR', 0, 0, 2, 0, 4),
  ]},
  { id: 'J', teams: [
    row('netherlands', 'Netherlands', 'NED', 1, 1, 0, 4, 2),
    row('belgium',     'Belgium',     'BEL', 1, 1, 0, 3, 2),
    row('iran',        'Iran',        'IRN', 0, 1, 1, 2, 3),
    row('qatar',       'Qatar',       'QAT', 0, 1, 1, 1, 3),
  ]},
  { id: 'K', teams: [
    row('croatia',  'Croatia',  'CRO', 1, 1, 0, 3, 2),
    row('scotland', 'Scotland', 'SCO', 1, 0, 1, 3, 3),
    row('sweden',   'Sweden',   'SWE', 1, 0, 1, 2, 2),
    row('austria',  'Austria',  'AUT', 0, 1, 1, 2, 3),
  ]},
  { id: 'L', teams: [
    row('switzerland',       'Switzerland',    'SUI', 1, 1, 0, 3, 2),
    row('turkiye',           'Türkiye',        'TUR', 1, 1, 0, 2, 1),
    row('czechia',           'Czech Republic', 'CZE', 0, 1, 1, 1, 2),
    row('bosnia-herzegovina','Bosnia & Herz.', 'BIH', 0, 1, 1, 1, 2),
  ]},
];
