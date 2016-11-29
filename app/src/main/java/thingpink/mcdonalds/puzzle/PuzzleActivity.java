package thingpink.mcdonalds.puzzle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.io.File;
import java.text.MessageFormat;

import thingpink.mcdonalds.puzzle.models.PuzzleModel;
import thingpink.mcdonalds.puzzle.utils.Utils;
import thingpink.mcdonalds.puzzle.views.PuzzleView;

/**
 * Created by Filipe Rodrigues on 29/11/2016.
 */

public class PuzzleActivity extends AppCompatActivity {
    protected static final int RESULT_SELECT_IMAGE = 0;
    protected static final int RESULT_TAKE_PHOTO = 1;
    protected static final int DEFAULT_SIZE = 3;

    private PuzzleView mPuzzleView;
    private PuzzleModel mPuzzle;
    private BitmapFactory.Options mBitmapOptions;
    private int mPuzzleWidth = 1;
    private int mPuzzleHeight = 1;
    private Uri mImageUri;
    private boolean isPortrait = true;
    private boolean isToMaintainAspectRatio = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mBitmapOptions = new BitmapFactory.Options();
        mBitmapOptions.inScaled = false;

        mPuzzle = new PuzzleModel();
        mPuzzleView = new PuzzleView(this, mPuzzle);
        setContentView(mPuzzleView);

        scramble();
        setPuzzleSize(DEFAULT_SIZE, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Bitmap puzzleImage = null;

        switch (requestCode) {
            case RESULT_SELECT_IMAGE: {
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    puzzleImage = Utils.loadBitmap(this, selectedImage, mPuzzleView.getWidth(), mPuzzleView.getHeight());
                }

                break;
            }
            case RESULT_TAKE_PHOTO: {
                if (resultCode == RESULT_OK) {
                    File file = new File(Utils.getSaveDirectory(), Globals.FILENAME_PHOTO);

                    if (file.exists()) {
                        Uri uri = Uri.fromFile(file);

                        if (uri != null) {
                            puzzleImage = Utils.loadBitmap(this, uri, mPuzzleView.getWidth(), mPuzzleView.getHeight());
                        }
                    }
                }
                break;
            }
        }

        if (puzzleImage == null) {
            Toast.makeText(this, getString(R.string.error_could_not_load_image), Toast.LENGTH_LONG).show();
        } else {
            setBitmap(puzzleImage);
        }
    }

    private void setBitmap(Bitmap bitmap) {
        mPuzzleView.setBitmap(bitmap);
        setPuzzleSize(Math.min(mPuzzleWidth, mPuzzleHeight), true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//		setRequestedOrientation(portrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void scramble() {
        mPuzzle.init(mPuzzleWidth, mPuzzleHeight);
        mPuzzle.scramble();
        mPuzzleView.invalidate();
    }

    protected void setPuzzleSize(int size, boolean scramble) {
        float ratio = Utils.getImageAspectRatio(mPuzzleView);

        if (ratio < 1) {
            ratio = 1f / ratio;
        }

        int newWidth;
        int newHeight;

        if (isToMaintainAspectRatio) {
            if (isPortrait) {
                newWidth = size;
                newHeight = (int) (size * ratio);
            } else {
                newWidth = (int) (size * ratio);
                newHeight = size;
            }
        } else {
            newWidth = size;
            newHeight = size;
        }

        if (scramble || newWidth != mPuzzleWidth || newHeight != mPuzzleHeight) {
            mPuzzleWidth = newWidth;
            mPuzzleHeight = newHeight;
            scramble();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.puzzle_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scramble:
                scramble();
                return true;

            case R.id.select_image:
                selectImage();
                return true;

            case R.id.take_photo:
                takePicture();
                return true;

            case R.id.change_tiling:
                changeTiling();
                return true;

            case R.id.stats:
                showStats();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onFinishPuzzle() {
        Utils.playSound(this, R.raw.solved_sound);
    }

    private void selectImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_SELECT_IMAGE);
    }

    private void takePicture() {
        File dir = Utils.getSaveDirectory();

        if (dir == null) {
            Toast.makeText(this, getString(R.string.error_creating_directory_to_store_photo), Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(dir, Globals.FILENAME_PHOTO);
        Intent photoPickerIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(photoPickerIntent, RESULT_TAKE_PHOTO);
    }

    private void changeTiling() {
        float ratio = Utils.getImageAspectRatio(mPuzzleView);

        if (ratio < 1) {
            ratio = 1f / ratio;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_tiling);

        String[] items = new String[Globals.MAX_WIDTH - Globals.MIN_WIDTH + 1];
        int selected = 0;

        for (int i = 0; i < items.length; i++) {
            int width;
            int height;

            if (isToMaintainAspectRatio) {
                if (isPortrait) {
                    width = i + Globals.MIN_WIDTH;
                    height = (int) (width * ratio);
                } else {
                    height = i + Globals.MIN_WIDTH;
                    width = (int) (height * ratio);
                }
            } else {
                if (isPortrait) {
                    width = i + Globals.MIN_WIDTH;
                    height = width;
                } else {
                    height = i + Globals.MIN_WIDTH;
                    width = height;
                }
            }

            items[i] = Utils.sizeToString(this, width, height);

            if (i + Globals.MIN_WIDTH == Math.min(mPuzzleWidth, mPuzzleHeight)) {
                selected = i;
            }
        }

        builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPuzzleSize(which + Globals.MIN_WIDTH, false);
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public void showStats() {
        String type = Utils.sizeToString(this, mPuzzleWidth, mPuzzleHeight);

        String msg;

        if (mPuzzle.isSolved()) {
            msg = MessageFormat.format(getString(R.string.finished_type_expert_puzzle_in_n_moves), type, mPuzzle.getMoveCount());
        } else {
            msg = getString(R.string.puzzle_n_moves_so_far).concat(String.valueOf(" " + mPuzzle.getMoveCount()));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(!mPuzzle.isSolved() ? R.string.title_stats : R.string.title_solved);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.label_ok, null);

        builder.create().show();
    }
}
