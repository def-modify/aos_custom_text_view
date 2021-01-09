import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Range;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomTextView extends AppCompatTextView {
    private Paint fontPaint;
    private Paint strokePaint;
    private int strokeWidth;

    public CustomTextView(Context context) {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fontPaint.setTextSize(getTextSize());

        Paint.FontMetrics fontMetrics = fontPaint.getFontMetrics();
        strokeWidth = Math.round((fontMetrics.bottom - fontMetrics.top + fontMetrics.leading) *  ((float)1 / 3));

        ArrayList<Range<Integer>> offsets = findApostrophe();

        Layout layout = getLayout();
        for (int line = 0; line < getLineCount(); ++line) {
            ArrayList<Rect> areas = getLineStrokes(layout, offsets, line);
            for (Rect area: areas) {
                strokePaint.setColor(new Random().nextInt());
                canvas.drawRect(area, strokePaint);
            }
        }

        super.onDraw(canvas);
    }

		private void init(Context context) {
        fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.RED);
		}

    private ArrayList<Range<Integer>> findApostrophe() {
        ArrayList<Range<Integer>> result = new ArrayList<>();

        String text = getText().toString();
        Pattern pattern = Pattern.compile("('(.|\\n|\\r)*?')");
        Matcher matcher = pattern.matcher(text);

        int offsetX = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            result.add(new Range<Integer>(offsetX + start, offsetX + end));

            offsetX += end;
            text = text.substring(end);

            matcher = pattern.matcher(text);
        }

        return result;
    }

    //  라인단위로 결과를 보냄
    private ArrayList<Range<Integer>> getStrokeRect(ArrayList<Range<Integer>> ranges, int start, int end) {
        ArrayList<Range<Integer>> result = new ArrayList<>();

        ArrayList<Range<Integer>> lineRanges = new ArrayList<>();
        for (Range<Integer> range: ranges) {
            if (range.contains(start) || range.contains(end)
            || (start <= range.getLower() && range.getUpper() <= end)) {
                lineRanges.add(range);
            }
        }

        for (Range<Integer> lineRange: lineRanges) {
            if (lineRange.getLower() < start) {
                //  오프셋이 이미 이전에 시작 됬음
                if (end < lineRange.getUpper()) {
                    //  종료 시점이 라인을 넘어감
                    result.add(new Range<Integer>(start, end));
                } else {
                    //  종료 지점은 라인 베이스
                    result.add(new Range<Integer>(start, lineRange.getUpper()));
                }
            } else {
                //  오프셋이 시작지점 이상 부터 시작
                if (end < lineRange.getUpper()) {
                    result.add(new Range<Integer>(lineRange.getLower(), end));
                } else {
                    result.add(new Range<Integer>(lineRange.getLower(), lineRange.getUpper()));
                }
            }
        }

        return result;
    }

    private ArrayList<Rect> getLineStrokes(Layout layout, ArrayList<Range<Integer>> offsets, int line) {
        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        String lineString = getText().subSequence(start, end).toString();

        Rect lineBound = new Rect();
        layout.getLineBounds(line, lineBound);

        //  spacing 은 있으면 잘 계산해서 넣으셈
//        int spacing = (int)(((lineBound.height() * getLineSpacingMultiplier()) + getLineSpacingExtra()) - lineBound.height());
        int top = layout.getLineTop(line);
        int bottom = layout.getLineBottom(line);

        int y = bottom - strokeWidth;

        ArrayList<Rect> areas = new ArrayList<>();
        ArrayList<Range<Integer>> ranges =  getStrokeRect(offsets, start, end);
        for (Range<Integer> range: ranges) {
            int x = (int)fontPaint.measureText(lineString, 0, range.getLower() - start);
            int width = (int)fontPaint.measureText(lineString, range.getLower() - start, range.getUpper() - start);

            areas.add(new Rect(x, y, x + width, y + strokeWidth));
        }

        return areas;
    }
}