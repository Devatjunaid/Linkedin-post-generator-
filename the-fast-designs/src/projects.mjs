// Portfolio data — drives the generated mockup artwork.
// NOTE: these are concept/sample projects. Replace with your real client work before launch.
const dark = { surface: '#11131A', surface2: '#171A23', chrome: '#1B1E28' };

export const PROJECTS = [
  {
    id: 'nimbus-pay', name: 'Nimbus Pay', type: 'Fintech dashboard', cat: 'webapp', layout: 'dash', domain: 'app.nimbuspay.io',
    bg: '#070B18', bg2: '#101A3A', ...dark, a: '#6C8CFF', b: '#22D3A6', onA: '#060A1A',
    kpi: '$1.28M', kpiDelta: '+24%', kpiLabel: 'Monthly volume',
    chart: { title: 'Nimbus Pay — 90 days after relaunch', labels: ['Sign‑ups', 'Activation', 'Retention', 'Speed score'], before: [100, 38, 52, 54], after: [212, 61, 74, 98] },
  },
  {
    id: 'ember-oak', name: 'Ember & Oak', type: 'Restaurant website', cat: 'website', layout: 'editorial', domain: 'emberandoak.com',
    bg: '#120A06', bg2: '#2A160C', ...dark, paper: '#F4EADB', ink: '#24140C', a: '#D9542B', b: '#E9C9A0', onA: '#fff',
    headline: ['Fire-cooked,', 'slow-served.'], cta: 'Reserve a table', points: ['Seasonal menu', 'Private dining', 'Wood-fired grill'], plate: true,
  },
  {
    id: 'voltline', name: 'Voltline', type: 'EV charging platform', cat: 'website', layout: 'saas', domain: 'voltline.energy',
    bg: '#04100B', bg2: '#0B2418', ...dark, a: '#C8FF2E', b: '#2EE6A6', onA: '#08140A',
    headline: ['Charge anywhere.', 'Pay in one tap.'], kpi: '3,412', kpiDelta: '+31%', kpiLabel: 'Chargers online',
    chart: { title: 'Voltline — 6 months after launch', labels: ['Organic traffic', 'Leads', 'Page speed', 'Avg. session'], before: [100, 42, 47, 61], after: [286, 117, 99, 138] },
  },
  {
    id: 'maison-lune', name: 'Maison Lune', type: 'Fashion e‑commerce', cat: 'ecommerce', layout: 'shop', domain: 'maisonlune.shop',
    bg: '#0F0A0E', bg2: '#2A1622', ...dark, a: '#F2B8C6', b: '#C9A27A', c: '#9C8CFF', onA: '#22121A', hero: '#FCE7EC',
    eyebrow: 'NEW SEASON', headline: ['Quiet luxury,', 'made to last.'], products: ['dress', 'bag', 'bottle', 'dress'],
    chart: { title: 'Maison Lune — first quarter on the new store', labels: ['Conversion', 'Avg. order', 'Mobile sales', 'Returning'], before: [100, 100, 100, 100], after: [168, 131, 214, 152] },
  },
  {
    id: 'pulse', name: 'Pulse Athletics', type: 'Fitness app & site', cat: 'webapp', layout: 'mobile', domain: 'pulseathletics.app',
    bg: '#0B0718', bg2: '#22104A', ...dark, a: '#FF3D7F', b: '#7B5CFF', onA: '#fff',
    headline: ['Train smarter.', 'Every single day.'],
  },
  {
    id: 'harbor-vale', name: 'Harbor & Vale', type: 'Law firm website', cat: 'website', layout: 'editorial', domain: 'harborvale.law',
    bg: '#060C10', bg2: '#122130', ...dark, paper: '#EEE9DF', ink: '#0F1C26', a: '#A9823E', b: '#1E3446', onA: '#fff',
    headline: ['Counsel that', 'stands firm.'], cta: 'Book a consultation', points: ['Corporate law', 'Real estate', 'Litigation'], menu: ['Practice', 'People', 'Insights', 'Contact'],
  },
  {
    id: 'atlas-trails', name: 'Atlas Trails', type: 'Travel booking site', cat: 'ecommerce', layout: 'shop', domain: 'atlastrails.travel',
    bg: '#041118', bg2: '#0A2A38', ...dark, a: '#3DD6FF', b: '#FFC857', c: '#7CFFB2', onA: '#03141C', hero: '#E8FBFF',
    eyebrow: 'SMALL-GROUP ADVENTURES', headline: ['Go further,', 'travel lighter.'], products: ['mountain', 'mountain', 'bag', 'mountain'],
  },
  {
    id: 'quanta', name: 'Quanta Labs', type: 'AI startup brand & site', cat: 'branding', layout: 'saas', domain: 'quantalabs.ai',
    bg: '#07060F', bg2: '#1A1236', ...dark, a: '#A78BFA', b: '#22D3EE', onA: '#0C0820',
    headline: ['Models that', 'explain themselves.'], kpi: '99.2%', kpiDelta: '+4.1%', kpiLabel: 'Prediction accuracy',
  },
  {
    id: 'greenroot', name: 'Greenroot Market', type: 'Grocery e‑commerce', cat: 'ecommerce', layout: 'shop', domain: 'greenroot.market',
    bg: '#06100A', bg2: '#13301C', ...dark, a: '#7BE36B', b: '#FFD166', c: '#FF8A5B', onA: '#06160A', hero: '#FF8A5B',
    eyebrow: 'FARM TO DOOR IN 24H', headline: ['Fresh food,', 'zero fuss.'], products: ['fruit', 'fruit', 'bottle', 'fruit'],
  },
];
