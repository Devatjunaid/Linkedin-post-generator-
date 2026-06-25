/* ---------- lightweight SVG area chart (recharts replacement) ---------- */
import React, { useState } from 'react';
import { View } from 'react-native';
import Svg, {
  Defs,
  LinearGradient,
  Path,
  Stop,
  Text as SvgText,
} from 'react-native-svg';
import { C, moneyShort } from '../theme';

type Point = { age: number; bal: number };

export function AreaChart({
  data,
  height = 160,
}: {
  data: Point[];
  height?: number;
}) {
  const [w, setW] = useState(0);

  const leftPad = 40;
  const rightPad = 10;
  const topPad = 10;
  const botPad = 18;

  const chartW = Math.max(0, w - leftPad - rightPad);
  const chartH = Math.max(0, height - topPad - botPad);

  let body: React.ReactNode = null;

  if (w > 0 && data.length > 1 && chartW > 0) {
    const ys = data.map(d => d.bal);
    let minY = Math.min(...ys);
    let maxY = Math.max(...ys);
    if (maxY === minY) {
      maxY = minY + 1;
    }

    const xAt = (i: number) => leftPad + (i / (data.length - 1)) * chartW;
    const yAt = (bal: number) =>
      topPad + (1 - (bal - minY) / (maxY - minY)) * chartH;

    const line = data
      .map((d, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i)} ${yAt(d.bal)}`)
      .join(' ');

    const bottom = topPad + chartH;
    const area = `${line} L ${xAt(data.length - 1)} ${bottom} L ${xAt(
      0,
    )} ${bottom} Z`;

    // y-axis ticks (top / middle / bottom)
    const ticks = [maxY, (maxY + minY) / 2, minY];

    body = (
      <Svg width={w} height={height}>
        <Defs>
          <LinearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
            <Stop offset="0%" stopColor={C.gold} stopOpacity={0.55} />
            <Stop offset="100%" stopColor={C.gold} stopOpacity={0.04} />
          </LinearGradient>
        </Defs>

        {ticks.map((t, i) => (
          <SvgText
            key={i}
            x={leftPad - 6}
            y={yAt(t) + 3}
            fill={C.muted}
            fontSize={10}
            textAnchor="end">
            {moneyShort(t)}
          </SvgText>
        ))}

        <Path d={area} fill="url(#areaFill)" />
        <Path
          d={line}
          fill="none"
          stroke={C.gold}
          strokeWidth={2.5}
          strokeLinejoin="round"
          strokeLinecap="round"
        />

        <SvgText
          x={leftPad}
          y={height - 4}
          fill={C.muted}
          fontSize={10}
          textAnchor="start">
          {data[0].age}
        </SvgText>
        <SvgText
          x={leftPad + chartW}
          y={height - 4}
          fill={C.muted}
          fontSize={10}
          textAnchor="end">
          {data[data.length - 1].age}
        </SvgText>
      </Svg>
    );
  }

  return (
    <View
      style={{ height }}
      onLayout={e => setW(e.nativeEvent.layout.width)}>
      {body}
    </View>
  );
}
