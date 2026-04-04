const NICK_ADJECTIVES = [
  'Brave',
  'Chill',
  'Clever',
  'Crafty',
  'Daring',
  'Fierce',
  'Happy',
  'Lucky',
  'Mighty',
  'Noble',
  'Quick',
  'Sneaky',
  'Swift',
  'Witty',
  'Zen',
];

const NICK_ANIMALS = [
  'Bear',
  'Eagle',
  'Falcon',
  'Fox',
  'Koala',
  'Lion',
  'Lynx',
  'Otter',
  'Owl',
  'Panda',
  'Raven',
  'Tiger',
  'Wolf',
  'Hawk',
  'Shark',
];

const PROJECT_PREFIXES = ['Sprint', 'Project', 'Release', 'Mission', 'Phase', 'Launch'];

const PROJECT_CODENAMES = [
  'Aurora',
  'Blaze',
  'Comet',
  'Eclipse',
  'Falcon',
  'Glacier',
  'Horizon',
  'Mercury',
  'Nova',
  'Olympus',
  'Phoenix',
  'Titan',
  'Vortex',
  'Zenith',
  'Apex',
];

const ROOM_ACTIVITIES = ['Planning', 'Refinement', 'Sizing', 'Review', 'Estimation', 'Grooming'];

const ROOM_CODENAMES = [
  'Alpha',
  'Bravo',
  'Charlie',
  'Delta',
  'Echo',
  'Foxtrot',
  'Golf',
  'Hotel',
  'India',
  'Juliet',
];

function pick(arr: readonly string[]): string {
  return arr[Math.floor(Math.random() * arr.length)] as string;
}

export function generateNickname(): string {
  return `${pick(NICK_ADJECTIVES)}${pick(NICK_ANIMALS)}`;
}

export function generateProjectName(): string {
  return `${pick(PROJECT_PREFIXES)} ${pick(PROJECT_CODENAMES)}`;
}

export function generateRoomName(): string {
  return `${pick(ROOM_ACTIVITIES)} ${pick(ROOM_CODENAMES)}`;
}
