const BENGALI_DIGITS: Record<string, string> = {
  '0': '০',
  '1': '১',
  '2': '২',
  '3': '৩',
  '4': '৪',
  '5': '৫',
  '6': '৬',
  '7': '৭',
  '8': '৮',
  '9': '৯',
};

export function toBengaliNumerals(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '';
  const str = value.toString();
  return str.replace(/[0-9]/g, (digit) => BENGALI_DIGITS[digit] || digit);
}

export function mapConditionToBengali(condition?: string | null, weatherCode?: number): string {
  if (!condition) return 'অজানা';
  const norm = condition.toLowerCase();

  if (norm.includes('thunder') || (weatherCode && weatherCode >= 200 && weatherCode < 300)) {
    return 'বজ্রসহ বৃষ্টি';
  }
  if (norm.includes('heavy rain') || (weatherCode && [502, 503, 504, 522].includes(weatherCode))) {
    return 'ভারী বৃষ্টি';
  }
  if (norm.includes('drizzle') || norm.includes('light rain') || (weatherCode && [300, 301, 310, 500].includes(weatherCode))) {
    return 'হালকা বৃষ্টি';
  }
  if (norm.includes('rain') || (weatherCode && weatherCode >= 500 && weatherCode < 600)) {
    return 'বৃষ্টি';
  }
  if (norm.includes('fog') || norm.includes('mist') || norm.includes('haze') || (weatherCode && [701, 721, 741].includes(weatherCode))) {
    return 'কুয়াশা';
  }
  if (norm.includes('dust') || norm.includes('sand') || (weatherCode && [731, 751, 761].includes(weatherCode))) {
    return 'ধুলোময়';
  }
  if (norm.includes('overcast')) {
    return 'মেঘাচ্ছন্ন';
  }
  if (norm.includes('mostly cloudy') || norm.includes('mainly cloudy')) {
    return 'প্রধানত মেঘলা';
  }
  if (norm.includes('partly cloudy') || norm.includes('scattered') || (weatherCode && [801, 802].includes(weatherCode))) {
    return 'আংশিক মেঘলা';
  }
  if (norm.includes('cloud') || (weatherCode && [803, 804].includes(weatherCode))) {
    return 'মেঘাচ্ছন্ন';
  }
  if (norm.includes('clear') || (weatherCode === 800)) {
    return 'পরিষ্কার আকাশ';
  }
  return condition;
}

export function getAqiCategory(aqi: number | null): { category: string; categoryBn: string } {
  if (aqi === null || aqi === undefined) {
    return { category: 'Unavailable', categoryBn: 'অনুপলব্ধ' };
  }
  if (aqi <= 50) {
    return { category: 'Good', categoryBn: 'ভালো' };
  }
  if (aqi <= 100) {
    return { category: 'Moderate', categoryBn: 'মাঝারি' };
  }
  if (aqi <= 150) {
    return { category: 'Unhealthy for Sensitive Groups', categoryBn: 'সংবেদনশীল গোষ্ঠীর জন্য অস্বাস্থ্যকর' };
  }
  if (aqi <= 200) {
    return { category: 'Unhealthy', categoryBn: 'অস্বাস্থ্যকর' };
  }
  if (aqi <= 300) {
    return { category: 'Very Unhealthy', categoryBn: 'খুব অস্বাস্থ্যকর' };
  }
  return { category: 'Hazardous', categoryBn: 'বিপজ্জনক' };
}

export function getMoonPhaseName(fraction: number): { en: string; bn: string } {
  // fraction 0 to 1: 0 = new moon, 0.25 = first quarter, 0.5 = full moon, 0.75 = third quarter
  if (fraction < 0.03 || fraction > 0.97) {
    return { en: 'New Moon', bn: 'অমাবস্যা' };
  }
  if (fraction < 0.22) {
    return { en: 'Waxing Crescent', bn: 'শুক্লপক্ষ' };
  }
  if (fraction < 0.28) {
    return { en: 'First Quarter', bn: 'শুক্লপক্ষ অর্ধচন্দ্র' };
  }
  if (fraction < 0.47) {
    return { en: 'Waxing Gibbous', bn: 'শুক্লপক্ষ কুব্জ' };
  }
  if (fraction < 0.53) {
    return { en: 'Full Moon', bn: 'পূর্ণিমা' };
  }
  if (fraction < 0.72) {
    return { en: 'Waning Gibbous', bn: 'কৃষ্ণপক্ষ কুব্জ' };
  }
  if (fraction < 0.78) {
    return { en: 'Last Quarter', bn: 'কৃষ্ণপক্ষ অর্ধচন্দ্র' };
  }
  return { en: 'Waning Crescent', bn: 'কৃষ্ণপক্ষ' };
}

export const BANGLADESH_DISTRICT_BN: Record<string, string> = {
  dhaka: 'ঢাকা',
  gazipur: 'গাজীপুর',
  narayanganj: 'নারায়ণগঞ্জ',
  tangail: 'টাঙ্গাইল',
  kishoreganj: 'কিশোরগঞ্জ',
  narsingdi: 'নরসিংদী',
  manikganj: 'মানিকগঞ্জ',
  munshiganj: 'মুন্সীগঞ্জ',
  faridpur: 'ফরিদপুর',
  gopalganj: 'গোপালগঞ্জ',
  madaripur: 'মাদারীপুর',
  rajbari: 'রাজবাড়ী',
  shariatpur: 'শরীয়তপুর',
  chattogram: 'চট্টগ্রাম',
  chittagong: 'চট্টগ্রাম',
  'cox\'s bazar': 'কক্সবাজার',
  coxsbazar: 'কক্সবাজার',
  cumilla: 'কুমিল্লা',
  comilla: 'কুমিল্লা',
  feni: 'ফেনী',
  brahmanbaria: 'ব্রাহ্মণবাড়িয়া',
  rangamati: 'রাঙ্গামাটি',
  noakhali: 'নোয়াখালী',
  chandpur: 'চাঁদপুর',
  lakshmipur: 'লক্ষ্মীপুর',
  khagrachhari: 'খাগড়াছড়ি',
  bandarban: 'বান্দরবান',
  rajshahi: 'রাজশাহী',
  bogura: 'বগুড়া',
  bogra: 'বগুড়া',
  pabna: 'পাবনা',
  sirajganj: 'সিরাজগঞ্জ',
  naogaon: 'নওগাঁ',
  natore: 'নাটোর',
  chapai_nawabganj: 'চাঁপাইনবাবগঞ্জ',
  joypurhat: 'জয়পুরহাট',
  khulna: 'খুলনা',
  jashore: 'যশোর',
  jessore: 'যশোর',
  satkhira: 'সাতক্ষীরা',
  kushtia: 'কুষ্টিয়া',
  jhenaidah: 'ঝিনাইদহ',
  bagerhat: 'বাগেরহাট',
  chuadanga: 'চুয়াডাঙ্গা',
  magura: 'মাগুরা',
  meherpur: 'মেহেরপুর',
  narail: 'নড়াইল',
  barishal: 'বরিশাল',
  barisal: 'বরিশাল',
  patuakhali: 'পটুয়াখালী',
  bhola: 'ভোলা',
  pirojpur: 'পিরোজপুর',
  barguna: 'বরগুনা',
  jhalokati: 'ঝালকাঠি',
  sylhet: 'সিলেট',
  moulvibazar: 'মৌলভীবাজার',
  habiganj: 'হবিগঞ্জ',
  sunamganj: 'সুনামগঞ্জ',
  rangpur: 'রংপুর',
  dinajpur: 'দিনাজপুর',
  kurigram: 'কুড়িগ্রাম',
  gaibandha: 'গাইবান্ধা',
  nilphamari: 'নীলফামারী',
  panchagarh: 'পঞ্চগড়',
  thakurgaon: 'ঠাকুরগাঁও',
  lalmonirhat: 'লালমনিরহাট',
  mymensingh: 'ময়মনসিংহ',
  jamalpur: 'জামালপুর',
  netrokona: 'নেত্রকোণা',
  sherpur: 'শেরপুর',
};

export const BANGLADESH_DIVISION_BN: Record<string, string> = {
  dhaka: 'ঢাকা',
  chattogram: 'চট্টগ্রাম',
  chittagong: 'চট্টগ্রাম',
  rajshahi: 'রাজশাহী',
  khulna: 'খুলনা',
  barishal: 'বরিশাল',
  barisal: 'বরিশাল',
  sylhet: 'সিলেট',
  rangpur: 'রংপুর',
  mymensingh: 'ময়মনসিংহ',
};
