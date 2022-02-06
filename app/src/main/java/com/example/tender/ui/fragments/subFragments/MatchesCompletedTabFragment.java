package com.example.tender.ui.fragments.subFragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.tender.R;
import com.example.tender.model.Match;
import com.example.tender.model.MatchWrapper;
import com.example.tender.model.User;
import com.example.tender.model.UserWrapper;
import com.example.tender.ui.RecyclerViewInterface;
import com.example.tender.ui.activity.MatchResultsPendingActivity;
import com.example.tender.ui.activity.SwipeActivity;
import com.example.tender.utils.adapter.recyclerAdapter.MatchesListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.todkars.shimmer.ShimmerRecyclerView;

import java.util.ArrayList;

public class MatchesCompletedTabFragment extends Fragment implements RecyclerViewInterface {
    private Context context;
    private ArrayList<MatchWrapper> completedMatches;
    private MatchesListAdapter matchesListAdapter;
    private ShimmerRecyclerView recyclerView;
    private TextView emptyView;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private ListenerRegistration registration;

    public MatchesCompletedTabFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        completedMatches = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_matches_completed_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.matches_completed_tab_recycler_view);
        matchesListAdapter = new MatchesListAdapter(context, firebaseUser.getUid(), completedMatches, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(matchesListAdapter);
        emptyView = view.findViewById(R.id.matches_completed_tab_empty_view);
        updateUI(true, false);
        startPolling();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
        stopPolling();
    }

    @Override
    public void onItemClick(int position) {
        MatchWrapper target = completedMatches.get(position);
        Intent intent = new Intent(context, MatchResultsPendingActivity.class);
        intent.putExtra("MATCH_ID", target.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(int position) {

    }

    private void startPolling() {
        registration = db.collection("users").document(firebaseUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    completedMatches.clear();

                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        updateUI(false, true);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        updateUI(false, true);
                        return;
                    }

                    User user = snapshot.toObject(User.class);
                    assert user != null;

                    if (user.getCompletedMatches() == null || user.getCompletedMatches().size() == 0) {
                        updateUI(false, true);
                        return;
                    }

                    db.collection("matches").whereIn(FieldPath.documentId(), user.getCompletedMatches())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    Match m = documentSnapshot.toObject(Match.class);
                                    completedMatches.add(new MatchWrapper(documentSnapshot.getId(), m));
                                }
                                updateUI(false, false);
                            })
                            .addOnFailureListener(e1 -> {
                                Log.w("TAG", "Load matches from ids failed.", e1);
                                updateUI(false, true);
                            });
                });
    }


    private void stopPolling() {
        if (registration != null) {
            registration.remove();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateUI(boolean showShimmer, boolean showEmptyView) {
        matchesListAdapter.notifyDataSetChanged();
        if (showShimmer) recyclerView.showShimmer();
        else recyclerView.hideShimmer();
        emptyView.setVisibility(showEmptyView ? View.VISIBLE : View.GONE);
    }
}