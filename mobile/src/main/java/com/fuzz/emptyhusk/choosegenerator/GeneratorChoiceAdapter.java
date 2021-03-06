/*
 * Copyright 2016 Philip Cohn-Cort
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fuzz.emptyhusk.choosegenerator;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fuzz.emptyhusk.BoldTextCellGenerator;
import com.fuzz.emptyhusk.R;
import com.fuzz.indicator.ImageCellGenerator;
import com.fuzz.indicator.CutoutCellGenerator;
import com.fuzz.indicator.clip.SequentialCellGenerator;
import com.fuzz.indicator.clip.ClippedImageCellGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Philip Cohn-Cort (Fuzz)
 */
public class GeneratorChoiceAdapter extends RecyclerView.Adapter {

    @NonNull
    private List<GeneratorChoice> choices = new ArrayList<>();

    @NonNull
    private Class<? extends CutoutCellGenerator> chosen = ImageCellGenerator.class;

    public GeneratorChoiceAdapter() {
        choices.add(new GeneratorChoice(ImageCellGenerator.class, "Creates simple ImageViews. These are offset with X or Y translations. A classic choice with static background and dynamic content."));
        choices.add(new GeneratorChoice(ClippedImageCellGenerator.class, "Creates ClippedImageViews. These are offset with X or Y translations. Stylish, yet ever so slightly heavier memory-wise than ImageCellGenerator."));
        choices.add(new GeneratorChoice(BoldTextCellGenerator.class, "Creates TextViews with bold text. The bold sections are offset in a dynamic manner within the TextViews' text. Rather new and difficult to master."));
        choices.add(new GeneratorChoice(SequentialCellGenerator.class, "Creates TextClippedImageViews. These clip backgrounds to strings and foregrounds to backgrounds. Minimalist design, with plenty of empty space."));
    }

    public void setChosen(@NonNull Class<? extends CutoutCellGenerator> chosen) {
        int oldPosition = indexOf(this.chosen);
        int newPosition = indexOf(chosen);
        this.chosen = chosen;

        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (newPosition != -1) {
            notifyItemChanged(newPosition);
        }
    }

    private int indexOf(Class<? extends CutoutCellGenerator> chosen) {
        int position = -1;
        for (int i = 0; i < choices.size(); i++) {
            GeneratorChoice choice = choices.get(i);
            if (choice.type == chosen) {
                position = i;
                break;
            }
        }
        return position;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.cell_generator, parent, false);
        return new GeneratorViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final GeneratorChoice choice = choices.get(position);
        GeneratorViewHolder generatorViewHolder = (GeneratorViewHolder) holder;
        generatorViewHolder.title.setText(choice.type.getSimpleName());
        generatorViewHolder.description.setText(choice.description);
        generatorViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosen != choice.type) {
                    setChosen(choice.type);
                }
            }
        });
        generatorViewHolder.setSelected(chosen == choice.type);
    }

    @NonNull
    public Class<? extends CutoutCellGenerator> getChosen() {
        return chosen;
    }

    @Override
    public int getItemCount() {
        return choices.size();
    }
}