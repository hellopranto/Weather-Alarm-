export interface ParsedSynop {
  temperatureC: number | null;
  dewPointC: number | null;
  feelsLikeC: number | null;
  tempMaxC: number | null;
  tempMinC: number | null;
  humidityPercent: number | null;
  pressureHpa: number | null;
  windSpeedKmh: number | null;
  windDirectionDegrees: number | null;
  visibilityKm: number | null;
  rainfallMm: number | null;
  weatherCondition: string | null;
  weatherCode: number | null;
  rawRbody: string;
}

export class SynopService {
  /**
   * Robust parser for BMD SYNOP / AAXX telegram and auxiliary observation notes.
   * Strictly returns null for values that cannot be parsed. No fabricated numbers.
   */
  parse(rawRbody: string, directHumidity?: string | number): ParsedSynop {
    const result: ParsedSynop = {
      temperatureC: null,
      dewPointC: null,
      feelsLikeC: null,
      tempMaxC: null,
      tempMinC: null,
      humidityPercent: null,
      pressureHpa: null,
      windSpeedKmh: null,
      windDirectionDegrees: null,
      visibilityKm: null,
      rainfallMm: null,
      weatherCondition: null,
      weatherCode: null,
      rawRbody: rawRbody || '',
    };

    if (!rawRbody || typeof rawRbody !== 'string') {
      if (directHumidity !== undefined && directHumidity !== null) {
        const dh = parseFloat(directHumidity.toString());
        if (!isNaN(dh) && dh >= 0 && dh <= 100) {
          result.humidityPercent = Math.round(dh);
        }
      }
      return result;
    }

    const text = rawRbody.trim();

    // 1. Direct text regex parsing for common BMD appended formats
    const rhMatch = text.match(/RH\s*[:=]\s*(\d+(?:\.\d+)?)\s*%/i) || text.match(/Humidity\s*[:=]\s*(\d+(?:\.\d+)?)/i);
    if (rhMatch) {
      const rh = parseFloat(rhMatch[1]);
      if (!isNaN(rh) && rh >= 0 && rh <= 100) {
        result.humidityPercent = Math.round(rh);
      }
    } else if (directHumidity !== undefined && directHumidity !== null) {
      const dh = parseFloat(directHumidity.toString());
      if (!isNaN(dh) && dh >= 0 && dh <= 100) {
        result.humidityPercent = Math.round(dh);
      }
    }

    const maxTempMatch = text.match(/Max(?:\.|\s+)?(?:temp|temperature)?\s*[:=]\s*([+-]?\d+(?:\.\d+)?)\s*°?C?/i);
    if (maxTempMatch) {
      const v = parseFloat(maxTempMatch[1]);
      if (!isNaN(v) && v > -20 && v < 65) {
        result.tempMaxC = Math.round(v * 10) / 10;
      }
    }

    const minTempMatch = text.match(/Min(?:\.|\s+)?(?:temp|temperature)?\s*[:=]\s*([+-]?\d+(?:\.\d+)?)\s*°?C?/i);
    if (minTempMatch) {
      const v = parseFloat(minTempMatch[1]);
      if (!isNaN(v) && v > -20 && v < 65) {
        result.tempMinC = Math.round(v * 10) / 10;
      }
    }

    const curTempMatch = text.match(/(?:Current\s+)?Temp(?:erature)?\s*[:=]\s*([+-]?\d+(?:\.\d+)?)\s*°?C?/i);
    if (curTempMatch) {
      const v = parseFloat(curTempMatch[1]);
      if (!isNaN(v) && v > -20 && v < 65) {
        result.temperatureC = Math.round(v * 10) / 10;
      }
    }

    const rainMatch = text.match(/Rain(?:fall)?\s*[:=]\s*(\d+(?:\.\d+)?)\s*(?:mm)?/i);
    if (rainMatch) {
      const v = parseFloat(rainMatch[1]);
      if (!isNaN(v) && v >= 0) {
        result.rainfallMm = Math.round(v * 10) / 10;
      }
    }

    const presMatch = text.match(/Pres(?:sure)?\s*[:=]\s*(\d+(?:\.\d+)?)\s*(?:hPa|mb)?/i);
    if (presMatch) {
      const v = parseFloat(presMatch[1]);
      if (!isNaN(v) && v >= 850 && v <= 1100) {
        result.pressureHpa = Math.round(v * 10) / 10;
      }
    }

    const windMatch = text.match(/Wind\s*[:=]\s*(\d+)\s*(?:kt|knots|km\/h|kmh|mps|m\/s)?/i);
    if (windMatch) {
      const rawSpeed = parseFloat(windMatch[1]);
      if (!isNaN(rawSpeed)) {
        if (/kt|knots/i.test(windMatch[0])) {
          result.windSpeedKmh = Math.round(rawSpeed * 1.852 * 10) / 10;
        } else {
          result.windSpeedKmh = Math.round(rawSpeed * 10) / 10;
        }
      }
    }

    // 2. Token-based SYNOP parsing (WMO FM 12-IX)
    const tokens = text.split(/\s+/);
    let inSection333 = false;

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i];

      if (token === '333') {
        inSection333 = true;
        continue;
      }

      // In Section 333 (Regional / Climatological data)
      if (inSection333) {
        // 1snTxTxTx : Maximum temperature for preceding period
        if (/^1[01]\d{3}$/.test(token) && result.tempMaxC === null) {
          const sign = token[1] === '1' ? -1 : 1;
          const val = (parseInt(token.substring(2), 10) / 10) * sign;
          if (val > -30 && val < 65) result.tempMaxC = Math.round(val * 10) / 10;
        }
        // 2snTnTnTn : Minimum temperature
        if (/^2[01]\d{3}$/.test(token) && result.tempMinC === null) {
          const sign = token[1] === '1' ? -1 : 1;
          const val = (parseInt(token.substring(2), 10) / 10) * sign;
          if (val > -30 && val < 65) result.tempMinC = Math.round(val * 10) / 10;
        }
        continue;
      }

      // In Section 1 (Standard surface observation)

      // 1snTTT : Air temperature (e.g., 10330 -> +33.0°C)
      if (/^1[01]\d{3}$/.test(token) && result.temperatureC === null) {
        const sign = token[1] === '1' ? -1 : 1;
        const val = (parseInt(token.substring(2), 10) / 10) * sign;
        if (val > -40 && val < 65) {
          result.temperatureC = Math.round(val * 10) / 10;
        }
      }

      // 2snTdTdTd : Dew-point temperature (e.g., 20240 -> +24.0°C)
      if (/^2[01]\d{3}$/.test(token) && result.dewPointC === null) {
        const sign = token[1] === '1' ? -1 : 1;
        const val = (parseInt(token.substring(2), 10) / 10) * sign;
        if (val > -40 && val < 65) {
          result.dewPointC = Math.round(val * 10) / 10;
        }
      }

      // 3P0P0P0P0 : Station level pressure (tenths of hPa)
      if (/^3\d{4}$/.test(token) && result.pressureHpa === null) {
        let p = parseInt(token.substring(1), 10) / 10;
        if (p < 500) p += 1000;
        if (p >= 850 && p <= 1100) {
          result.pressureHpa = Math.round(p * 10) / 10;
        }
      }

      // 4PPPP : Mean sea-level pressure (tenths of hPa, e.g. 40098 -> 1009.8 hPa)
      if (/^4\d{4}$/.test(token)) {
        let p = parseInt(token.substring(1), 10) / 10;
        if (p < 500) p += 1000;
        if (p >= 850 && p <= 1100) {
          result.pressureHpa = Math.round(p * 10) / 10;
        }
      }

      // Nddff : Cloud cover, Wind direction, Wind speed
      // Valid before 1snTTT group, 5 digits
      if (/^[\d/](\d{2})(\d{2})$/.test(token) && i >= 3 && i <= 6 && result.windSpeedKmh === null) {
        const dd = parseInt(token.substring(1, 3), 10);
        const ff = parseInt(token.substring(3, 5), 10);
        if (dd >= 0 && dd <= 36 && ff >= 0 && ff <= 99) {
          result.windDirectionDegrees = dd === 36 ? 360 : dd * 10;
          // Typically in knots in BMD AAXX, convert to km/h
          result.windSpeedKmh = Math.round(ff * 1.852 * 10) / 10;
        }
      }

      // 6RRRtR : Rainfall group
      if (/^6(\d{3})\d$/.test(token) && result.rainfallMm === null) {
        const rrr = parseInt(token.substring(1, 4), 10);
        if (rrr === 990) {
          result.rainfallMm = 0.0; // Trace
        } else if (rrr >= 991 && rrr <= 999) {
          result.rainfallMm = Math.round((rrr - 990) * 0.1 * 10) / 10;
        } else if (rrr <= 989) {
          result.rainfallMm = rrr;
        }
      }

      // 7wwW1W2 : Present and past weather
      if (/^7(\d{2})\d{2}$/.test(token) && result.weatherCode === null) {
        const ww = parseInt(token.substring(1, 3), 10);
        const parsed = this.mapPresentWeatherCode(ww);
        result.weatherCode = parsed.code;
        result.weatherCondition = parsed.condition;
      }

      // iRixhVV : Visibility
      if (/^[0-4][1-3][0-9/](\d{2})$/.test(token) && result.visibilityKm === null) {
        const vv = parseInt(token.substring(3, 5), 10);
        if (vv <= 50) {
          result.visibilityKm = Math.round((vv * 0.1) * 10) / 10;
        } else if (vv >= 56 && vv <= 80) {
          result.visibilityKm = vv - 50;
        } else if (vv >= 90) {
          result.visibilityKm = vv === 98 ? 35 : vv === 99 ? 50 : 10;
        }
      }
    }

    // 3. If relative humidity is still null, calculate from Temp and Dewpoint using Magnus-Tetens formula
    if (
      result.humidityPercent === null &&
      result.temperatureC !== null &&
      result.dewPointC !== null
    ) {
      const T = result.temperatureC;
      const Td = result.dewPointC;
      if (T >= -30 && T <= 60 && Td >= -40 && Td <= 60 && Td <= T + 1) {
        const a = 17.625;
        const b = 243.04;
        const alpha = (a * T) / (b + T);
        const beta = (a * Td) / (b + Td);
        const rh = 100 * Math.exp(beta - alpha);
        if (!isNaN(rh) && rh >= 0 && rh <= 100) {
          result.humidityPercent = Math.min(100, Math.max(0, Math.round(rh)));
        }
      }
    }

    // 4. Compute Feels Like (Heat Index) if temperature & humidity available
    if (result.temperatureC !== null && result.humidityPercent !== null) {
      result.feelsLikeC = this.calculateFeelsLike(result.temperatureC, result.humidityPercent);
    } else if (result.temperatureC !== null) {
      result.feelsLikeC = result.temperatureC;
    }

    return result;
  }

  private mapPresentWeatherCode(ww: number): { condition: string; code: number } {
    if (ww >= 95) return { condition: 'Thunderstorm', code: 211 };
    if (ww >= 80 && ww <= 82) return { condition: 'Rain Showers', code: 521 };
    if (ww >= 60 && ww <= 65) return { condition: 'Rain', code: 501 };
    if (ww >= 50 && ww <= 59) return { condition: 'Drizzle', code: 301 };
    if ((ww >= 40 && ww <= 49) || ww === 10) return { condition: 'Fog', code: 741 };
    if (ww >= 4 && ww <= 9) return { condition: 'Haze', code: 721 };
    if (ww === 3) return { condition: 'Overcast', code: 804 };
    if (ww === 2) return { condition: 'Cloudy', code: 803 };
    if (ww === 1) return { condition: 'Partly Cloudy', code: 802 };
    if (ww === 0) return { condition: 'Clear', code: 800 };
    return { condition: 'Clear', code: 800 };
  }

  private calculateFeelsLike(tempC: number, humidity: number): number {
    // Steadman heat index approximation
    if (tempC < 20) return Math.round(tempC * 10) / 10;
    const T = tempC;
    const R = humidity;
    const c1 = -8.78469475556;
    const c2 = 1.61139411;
    const c3 = 2.33854883889;
    const c4 = -0.14611605;
    const c5 = -0.012308094;
    const c6 = -0.0164248277778;
    const c7 = 0.002211732;
    const c8 = 0.00072546;
    const c9 = -0.000003582;

    const hi =
      c1 +
      c2 * T +
      c3 * R +
      c4 * T * R +
      c5 * T * T +
      c6 * R * R +
      c7 * T * T * R +
      c8 * T * R * R +
      c9 * T * T * R * R;

    return Math.round(hi * 10) / 10;
  }
}
