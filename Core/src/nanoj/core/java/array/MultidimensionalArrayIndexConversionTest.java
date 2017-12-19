package nanoj.core.java.array;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 27/11/2013
 * Time: 16:51
 */
public class MultidimensionalArrayIndexConversionTest {
    Random rand = new Random();
    int p, x, y, z, t, xs, ys, zs, ts;

    @Before
    public void setUp() throws Exception {
        xs = rand.nextInt(30)+2;
        ys = rand.nextInt(30)+2;
        zs = rand.nextInt(30)+2;
        ts = rand.nextInt(30)+2;
        x = rand.nextInt(xs-1);
        y = rand.nextInt(ys-1);
        z = rand.nextInt(zs-1);
        t = rand.nextInt(ts-1);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void MultiDimToOneDim() throws Exception {
        p = 0;
        for (int y_=0; y_<ys; y_++){
            for (int x_=0; x_<xs; x_++){

                if (x_ == x && y_ == y)
                {
                    x_ = xs;
                    y_ = ys;
                    break;
                }
                p++;
            }
        }
        assert MultidimensionalArrayIndexConversion.convert2Dto1D(x, y, xs, ys) == p;

        p = 0;
        for (int z_=0; z_<zs; z_++){
            for (int y_=0; y_<ys; y_++){
                for (int x_=0; x_<xs; x_++){

                    if (x_ == x && y_ == y && z_ == z)
                    {
                        x_ = xs;
                        y_ = ys;
                        z_ = zs;
                        break;
                    }
                    p++;
                }
            }
        }
        assert MultidimensionalArrayIndexConversion.convert3Dto1D(x, y, z, xs, ys, zs) == p;

        p = 0;
        for (int t_=0; t_<ts; t_++){
            for (int z_=0; z_<zs; z_++){
                for (int y_=0; y_<ys; y_++){
                    for (int x_=0; x_<xs; x_++){
                        if (x_ == x && y_ == y && z_ == z && t_ == t)
                        {
                            x_ = xs;
                            y_ = ys;
                            z_ = zs;
                            t_ = ts;
                            break;
                        }
                        p++;
                    }
                }
            }
        }
        assert MultidimensionalArrayIndexConversion.convert4Dto1D(x, y, z, t, xs, ys, zs, ts) == p;
    }

    @Test
    public void OneDimToMultiDim() throws Exception {

        int r[];

        p = 0;
        for (int y_=0; y_<ys; y_++){
            for (int x_=0; x_<xs; x_++){

                if (x_ == x && y_ == y)
                {
                    x_ = xs;
                    y_ = ys;
                    break;
                }
                p++;
            }
        }
        r = MultidimensionalArrayIndexConversion.convert1Dto2D(p, xs, ys);
        assert (r[0] == x && r[1] == y);

        p = 0;
        for (int z_=0; z_<zs; z_++){
            for (int y_=0; y_<ys; y_++){
                for (int x_=0; x_<xs; x_++){

                    if (x_ == x && y_ == y && z_ == z)
                    {
                        x_ = xs;
                        y_ = ys;
                        z_ = zs;
                        break;
                    }
                    p++;
                }
            }
        }
        r = MultidimensionalArrayIndexConversion.convert1Dto3D(p, xs, ys, zs);
        assert (r[2] == z);
        assert (r[1] == y);
        assert (r[0] == x);

    }

}