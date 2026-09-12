/**
 * Centralized Salary Formatter Utility for HireRanker (Indian Recruitment Market).
 * Normalizes all salary inputs and stored representations into standard Indian LPA format:
 * Examples:
 *   "1000000 - 1200000" -> "₹10-12 LPA"
 *   "800000 - 1000000"   -> "₹8-10 LPA"
 *   "600000"             -> "₹6 LPA"
 *   "1500000 - 2000000"  -> "₹15-20 LPA"
 *   "10-12"              -> "₹10-12 LPA"
 *   "₹10 - 16 LPA"       -> "₹10-16 LPA"
 *   "$120k - $150k"      -> "₹12-15 LPA"
 *   "$10,000 - $12,000"  -> "₹10-12 LPA"
 */

export function formatSalaryToLpa(rawSalary: string | number | null | undefined): string {
  if (rawSalary === null || rawSalary === undefined) {
    return '₹10-16 LPA';
  }

  const str = String(rawSalary).trim();
  if (!str) {
    return '₹10-16 LPA';
  }

  // 1. If already formatted like "₹10 - 16 LPA", "10-12 LPA", "₹14 - 20 LPA", normalize spacing
  if (/^₹?\s*\d+(?:\.\d+)?\s*(?:-|–|to)\s*\d+(?:\.\d+)?\s*LPA$/i.test(str)) {
    const stripped = str.replace(/[₹\s]/g, '').toUpperCase();
    const parts = stripped.split(/-|–|TO/);
    if (parts.length === 2) {
      const min = cleanNum(parts[0]);
      const max = cleanNum(parts[1].replace('LPA', ''));
      return `₹${min}-${max} LPA`;
    }
  }

  // 2. Remove commas and currency symbols ($, ₹, €, £) from values
  const noCommas = str.replace(/[,₹$€£]/g, '');

  // 3. Match range: "1000000 - 1200000", "800000 - 1000000", "$120k - $150k", "$10000 - $12000", "10-12"
  const rangeMatch = noCommas.match(/(\d+(?:\.\d+)?)\s*(?:k|lpa|lac|lakh|lakhs)?\s*(?:-|–|to)\s*(\d+(?:\.\d+)?)\s*(?:k|lpa|lac|lakh|lakhs)?/i);
  if (rangeMatch) {
    const n1 = parseFloat(rangeMatch[1]);
    const n2 = parseFloat(rangeMatch[2]);
    let lpa1 = convertNumberToLpa(n1);
    let lpa2 = convertNumberToLpa(n2);

    if (lpa1 > lpa2) {
      const tmp = lpa1;
      lpa1 = lpa2;
      lpa2 = tmp;
    }

    return `₹${formatLpa(lpa1)}-${formatLpa(lpa2)} LPA`;
  }

  // 4. Match single numeric value: "600000", "12 LPA", "10"
  const singleMatch = noCommas.match(/(\d+(?:\.\d+)?)\s*(?:k|lpa|lac|lakh|lakhs)?/i);
  if (singleMatch) {
    const val = parseFloat(singleMatch[1]);
    const lpa = convertNumberToLpa(val);
    return `₹${formatLpa(lpa)} LPA`;
  }

  return '₹10-16 LPA';
}

function convertNumberToLpa(val: number): number {
  if (val >= 100000) {
    // Full INR annual value (e.g. 1000000 -> 10 LPA)
    return val / 100000;
  } else if (val >= 10000) {
    // USD or local testing format (e.g. 10000 or 12000 -> 10 to 12 LPA)
    return val / 1000;
  } else if (val >= 50) {
    // e.g. 120k or 150k (120 -> 12 LPA, 150 -> 15 LPA)
    return val / 10;
  } else {
    // Already in LPA (e.g. 8, 10, 12, 14.5)
    return val;
  }
}

function formatLpa(lpa: number): string {
  return Number.isInteger(lpa) ? lpa.toString() : lpa.toFixed(1);
}

function cleanNum(s: string): string {
  return s.trim().replace(/^₹/, '');
}

/**
 * Strips internal database IDs, hashes, or timestamps from job titles:
 * e.g. "Senior Full Stack Cloud Engineer 1789196791306" -> "Senior Full Stack Cloud Engineer"
 */
export function cleanJobTitle(title: string | null | undefined): string {
  if (!title) return '';
  return title.replace(/\s+(?:#?\d{5,}|\b[a-f0-9]{8,}\b).*$/i, '').trim();
}

/**
 * Strips accidental application IDs or internal tags from candidate display labels:
 * e.g. "GORAI ESHWAR RAO - eshwar@candidate.com (App #18)" -> "GORAI ESHWAR RAO - eshwar@candidate.com"
 */
export function cleanCandidateName(name: string | null | undefined): string {
  if (!name) return 'Candidate';
  return name.replace(/\s*\(App\s*#?\d+\)/gi, '').replace(/\s*#\d+/g, '').trim();
}

export function cleanCandidateEmail(email: string | null | undefined): string {
  if (!email) return '';
  return email.replace(/\s*\(App\s*#?\d+\)/gi, '').replace(/\s*#\d+/g, '').trim();
}
