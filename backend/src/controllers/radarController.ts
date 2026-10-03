import { Request, Response } from 'express';
import { RainViewerProvider } from '../providers/rainviewer/rainViewerProvider';

const rainViewer = new RainViewerProvider();

export const getRadarMetadata = async (_req: Request, res: Response) => {
  try {
    const radarData = await rainViewer.getRadarData();
    res.json({
      success: true,
      provider: 'RainViewer',
      version: radarData.version,
      generated: radarData.generated,
      host: radarData.host,
      past: radarData.radar?.past || [],
      nowcast: radarData.radar?.nowcast || [],
      satellite: radarData.satellite?.infrared || [],
      tileUrlTemplate: `${radarData.host}{path}/256/{z}/{x}/{y}/2/1_1.png`,
    });
  } catch (error: any) {
    res.status(503).json({
      success: false,
      error: 'RainViewer radar data temporarily unavailable',
      message: error.message,
    });
  }
};
