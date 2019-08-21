package pt.ulisboa.tecnico.cmu.locmess.features.posts.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessLinkedHashSet;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private LocMessLinkedHashSet mPosts;
    private final ListItemClickListener mListItemClickListener;

    public interface ListItemClickListener {
        void onListItemClick(PostLocMess post);
    }

    public PostAdapter(LocMessLinkedHashSet posts, ListItemClickListener listener) {
        super();
        mPosts = posts;
        mListItemClickListener = listener;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_post_list, parent, false);

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        holder.bind(mPosts.get(position));
    }

    @Override
    public int getItemCount() {
        if (mPosts == null) return 0;
        else return mPosts.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private PostLocMess mPost;
        private TextView mLocation;
        private TextView mSubject;
        private TextView mCreationDate;
        private TextView mContent;

        public PostViewHolder(View itemView) {
            super(itemView);

            mLocation = (TextView) itemView.findViewById(R.id.tv_post_location);
            mSubject = (TextView) itemView.findViewById(R.id.tv_post_subject);
            mCreationDate = (TextView) itemView.findViewById(R.id.tv_post_date_time);
            mContent = (TextView) itemView.findViewById(R.id.tv_post_content);

            itemView.setOnClickListener(this);
        }

        public void bind(Object post) {
            mPost = (PostLocMess) post;

            mLocation.setText(mPost.getLocation().getName());

            mCreationDate.setText(mPost.getCreationDate().toString("dd/MM/yyyy"));

            mSubject.setSingleLine();
            mSubject.setEllipsize(TextUtils.TruncateAt.END);
            mSubject.setText(mPost.getSubject());

            mContent.setMaxLines(2);
            mContent.setEllipsize(TextUtils.TruncateAt.END);
            mContent.setText(mPost.getContent());
        }

        @Override
        public void onClick(View v) {
            mListItemClickListener.onListItemClick(mPost);
        }
    }
}
